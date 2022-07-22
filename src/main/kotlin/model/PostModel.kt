package model

data class PostModel(
    val post: Post,
    val author: Author,
    val comments: List<Comment>
)
