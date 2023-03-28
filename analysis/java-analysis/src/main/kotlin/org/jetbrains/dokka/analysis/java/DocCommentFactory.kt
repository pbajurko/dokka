package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiNamedElement

class DocCommentFactory(
    private val docCommentCreators: List<DocCommentCreator>
) {
    fun fromElement(element: PsiNamedElement): DocComment? {
        docCommentCreators.forEach { creator ->
            val comment = creator.create(element)
            if (comment != null) {
                return comment
            }
        }
        return null
    }
}
