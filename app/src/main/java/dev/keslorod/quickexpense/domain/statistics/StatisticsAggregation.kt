package dev.keslorod.quickexpense.domain.statistics

import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.Tag

data class CategoryAmountFragment(
    val expenseId: String,
    val splitNodeId: String?,
    val amount: Long,
    val effectiveCategoryId: String,
    val label: String,
)

data class TagAmountFragment(
    val expenseId: String,
    val splitNodeId: String,
    val tagId: String,
    val amount: Long,
)

object StatisticsAggregation {

    fun buildCategoryFragments(
        expense: Expense,
        allSplitNodes: List<SplitNode>
    ): List<CategoryAmountFragment> {
        val expenseNodes = allSplitNodes.filter { it.expenseId == expense.id }
        if (expenseNodes.isEmpty()) {
            return listOf(
                CategoryAmountFragment(
                    expenseId = expense.id,
                    splitNodeId = null,
                    amount = expense.amount,
                    effectiveCategoryId = expense.categoryId,
                    label = expense.note ?: "Expense"
                )
            )
        }

        val fragments = mutableListOf<CategoryAmountFragment>()
        val rootNodes = expenseNodes.filter { it.parentId == null }
        
        rootNodes.forEach { node ->
            collectFragmentsFromNode(node, expenseNodes, expense.categoryId, fragments)
        }
        
        // Handle unallocated amount at root if any
        val totalAllocated = rootNodes.sumOf { it.amount }
        if (expense.amount > totalAllocated) {
            fragments.add(
                CategoryAmountFragment(
                    expenseId = expense.id,
                    splitNodeId = null,
                    amount = expense.amount - totalAllocated,
                    effectiveCategoryId = expense.categoryId,
                    label = "Unallocated"
                )
            )
        }

        return fragments
    }

    private fun collectFragmentsFromNode(
        node: SplitNode,
        allNodes: List<SplitNode>,
        inheritedCategoryId: String,
        result: MutableList<CategoryAmountFragment>
    ) {
        val children = allNodes.filter { it.parentId == node.id }
        val currentCategoryId = node.categoryId ?: inheritedCategoryId
        
        if (children.isEmpty()) {
            result.add(
                CategoryAmountFragment(
                    expenseId = node.expenseId,
                    splitNodeId = node.id,
                    amount = node.amount,
                    effectiveCategoryId = currentCategoryId,
                    label = node.label ?: "Item"
                )
            )
        } else {
            children.forEach { child ->
                collectFragmentsFromNode(child, allNodes, currentCategoryId, result)
            }
            
            // Handle unallocated amount within this node
            val totalAllocated = children.sumOf { it.amount }
            if (node.amount > totalAllocated) {
                result.add(
                    CategoryAmountFragment(
                        expenseId = node.expenseId,
                        splitNodeId = node.id,
                        amount = node.amount - totalAllocated,
                        effectiveCategoryId = currentCategoryId,
                        label = (node.label ?: "Item") + " (rest)"
                    )
                )
            }
        }
    }

    fun buildTagFragments(
        allSplitNodes: List<SplitNode>,
        nodeTags: Map<String, List<Tag>>
    ): List<TagAmountFragment> {
        val fragments = mutableListOf<TagAmountFragment>()
        allSplitNodes.forEach { node ->
            val tags = nodeTags[node.id].orEmpty()
            tags.forEach { tag ->
                fragments.add(
                    TagAmountFragment(
                        expenseId = node.expenseId,
                        splitNodeId = node.id,
                        tagId = tag.id,
                        amount = node.amount
                    )
                )
            }
        }
        return fragments
    }
}
