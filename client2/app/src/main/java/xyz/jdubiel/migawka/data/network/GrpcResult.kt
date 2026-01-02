package xyz.jdubiel.migawka.data.network

sealed interface GrpcResult<out T> {
    data class Success<out T>(val data: T) : GrpcResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : GrpcResult<Nothing>
}
