package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.JavaDocElementType
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.analysis.java.DescriptionJavadocTag
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.dokka.analysis.java.parsers.resolveToGetDri

internal fun PsiDocComment.hasTag(tag: JavadocTag): Boolean =
    when (tag) {
        DescriptionJavadocTag -> descriptionElements.isNotEmpty()
        else -> findTagByName(tag.name) != null
    }

internal fun PsiDocTag.contentElementsWithSiblingIfNeeded(): List<PsiElement> = if (dataElements.isNotEmpty()) {
    listOfNotNull(
        dataElements[0],
        dataElements[0].nextSibling?.takeIf { it.text != dataElements.drop(1).firstOrNull()?.text },
        *dataElements.drop(1).toTypedArray()
    )
} else {
    emptyList()
}

internal fun PsiDocTag.resolveToElement(): PsiElement? =
    dataElements.firstOrNull()?.firstChild?.referenceElementOrSelf()?.resolveToGetDri()

fun PsiDocTag.referenceElement(): PsiElement? =
    linkElement()?.referenceElementOrSelf()

internal fun PsiElement.referenceElementOrSelf(): PsiElement? =
    if (node.elementType == JavaDocElementType.DOC_REFERENCE_HOLDER) {
        PsiTreeUtil.findChildOfType(this, PsiJavaCodeReferenceElement::class.java)
    } else this

fun PsiDocTag.linkElement(): PsiElement? =
    valueElement ?: dataElements.firstOrNull { it !is PsiWhiteSpace }

fun PsiElement.defaultLabel() = children.firstOrNull {
    it is PsiDocToken && it.text.isNotBlank() && !it.isSharpToken()
} ?: this

fun PsiDocToken.isSharpToken() = tokenType == JavaDocTokenType.DOC_TAG_VALUE_SHARP_TOKEN