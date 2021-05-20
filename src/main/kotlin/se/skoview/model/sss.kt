package se.skoview.model

data class xBbTagPagination(
    val next: String = "",
    val values: List<BbTag>?,
    val page: Int = 0,
    val pagelen: Int = 0
)

data class xCommits(val href: String = "")


data class xComments(val href: String = "")

data class xHtml(val href: String = "")

data class xSelf(val href: String = "")

data class xAvatar(val href: String = "")


data class xLinks(
    val self: Self,
    val html: Html
)


