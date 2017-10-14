package org.sillymoo.symantest.model

/**
 * API representation of a Github repository.
 */
data class Repository (
    val id: String,
    val name: String,
    val url: String,
    val owner: String)
