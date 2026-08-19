package me.proxer.app.anime.resolver

/**
 * @author Ruben Gees
 */
object StreamResolverFactory {

    private val resolvers = arrayOf(
        MessageStreamResolver,
        ProxerStreamResolver,
        ProxerStreamCFResolver
    )

    fun resolverFor(name: String) = resolvers.find { it.supports(name) }
}
