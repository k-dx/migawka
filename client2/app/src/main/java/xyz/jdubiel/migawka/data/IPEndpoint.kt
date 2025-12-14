package xyz.jdubiel.migawka.data

data class IPEndpoint(
    val ip: String,
    val port: Int
) {
    init {
        require(ip.isNotBlank()) { "ip must not be blank" }
        require(port in 0..65535) { "port must be in 0..65535" }
    }

    override fun toString(): String = "$ip:$port"
}

