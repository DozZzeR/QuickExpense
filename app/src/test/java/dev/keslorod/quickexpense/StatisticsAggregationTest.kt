package dev.keslorod.quickexpense

import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.statistics.StatisticsAggregation
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsAggregationTest {

    private fun expense(id: String, amount: Long, categoryId: String = "cat_food") =
        Expense(id = id, amount = amount, sourceId = "src", categoryId = categoryId)

    @Test
    fun unsplitExpenseProducesSingleFragmentWithExpenseCategory() {
        val exp = expense("e1", 10_000, categoryId = "cat_food")
        val fragments = StatisticsAggregation.buildCategoryFragments(exp, emptyList())
        assertEquals(1, fragments.size)
        assertEquals(10_000, fragments.first().amount)
        assertEquals("cat_food", fragments.first().effectiveCategoryId)
    }

    @Test
    fun rootUnallocatedRemainderBecomesItsOwnFragment() {
        val exp = expense("e1", 10_000, categoryId = "cat_food")
        val nodes = listOf(
            SplitNode(id = "n1", expenseId = "e1", parentId = null, amount = 6_000, categoryId = "cat_alcohol", depth = 0)
        )
        val fragments = StatisticsAggregation.buildCategoryFragments(exp, nodes)
        // 6000 alcohol leaf + 4000 unallocated (falls back to expense category).
        assertEquals(6_000, fragments.filter { it.effectiveCategoryId == "cat_alcohol" }.sumOf { it.amount })
        assertEquals(4_000, fragments.filter { it.effectiveCategoryId == "cat_food" }.sumOf { it.amount })
        assertEquals(10_000, fragments.sumOf { it.amount })
    }

    @Test
    fun childWithNullCategoryInheritsNearestAncestorCategory() {
        // Expense total equals the root node so there is no root-level unallocated remainder.
        val exp = expense("e1", 6_000, categoryId = "cat_root")
        val nodes = listOf(
            SplitNode(id = "food", expenseId = "e1", parentId = null, amount = 6_000, categoryId = "cat_food", depth = 0),
            SplitNode(id = "sausage", expenseId = "e1", parentId = "food", amount = 6_000, categoryId = null, depth = 1)
        )
        val fragments = StatisticsAggregation.buildCategoryFragments(exp, nodes)
        // Only the leaf counts (no double counting of the parent), and it inherits cat_food.
        assertEquals(1, fragments.size)
        assertEquals("cat_food", fragments.first().effectiveCategoryId)
        assertEquals(6_000, fragments.first().amount)
    }

    @Test
    fun leafNodesAreCountedNotTheirParents() {
        val exp = expense("e1", 10_000)
        val nodes = listOf(
            SplitNode(id = "p", expenseId = "e1", parentId = null, amount = 10_000, categoryId = "cat_food", depth = 0),
            SplitNode(id = "c1", expenseId = "e1", parentId = "p", amount = 4_000, depth = 1),
            SplitNode(id = "c2", expenseId = "e1", parentId = "p", amount = 6_000, depth = 1)
        )
        val fragments = StatisticsAggregation.buildCategoryFragments(exp, nodes)
        // Parent fully allocated by children -> total stays 10_000, no double count.
        assertEquals(10_000, fragments.sumOf { it.amount })
    }

    @Test
    fun tagFragmentsOverlapEachTagGetsFullNodeAmount() {
        val nodes = listOf(
            SplitNode(id = "beer", expenseId = "e1", parentId = null, amount = 1_200, depth = 0)
        )
        val nodeTags = mapOf(
            "beer" to listOf(
                Tag(id = "t_beer", name = "beer", normalizedName = "beer"),
                Tag(id = "t_guests", name = "guests", normalizedName = "guests")
            )
        )
        val fragments = StatisticsAggregation.buildTagFragments(nodes, nodeTags)
        assertEquals(2, fragments.size)
        // Overlapping: each tag receives the full node amount.
        assertEquals(1_200, fragments.first { it.tagId == "t_beer" }.amount)
        assertEquals(1_200, fragments.first { it.tagId == "t_guests" }.amount)
    }
}
