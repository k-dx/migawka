package xyz.jdubiel.migawka.data.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import xyz.jdubiel.migawka.MigawkaGrpcKt
import java.util.concurrent.TimeUnit

class GrpcProvider(private val remoteEndpoint: IPEndpoint) {
    private var channel: ManagedChannel? = null
    private var migawkaServiceStub: MigawkaGrpcKt.MigawkaCoroutineStub? = null

    @Synchronized
    fun getChannel(): ManagedChannel {
        if (channel == null) {
            channel = ManagedChannelBuilder
                .forAddress(remoteEndpoint.ip, remoteEndpoint.port)
                .usePlaintext() // TODO: use TLS, not plaintext!
                .build()
        }
        return channel!!
    }

    @Synchronized
    fun getMigawkaServiceStub(): MigawkaGrpcKt.MigawkaCoroutineStub {
        if (migawkaServiceStub == null) {
            migawkaServiceStub = MigawkaGrpcKt.MigawkaCoroutineStub(getChannel())
        }
        return migawkaServiceStub!!
    }

    @Synchronized
    fun shutdown() {
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        channel = null
        migawkaServiceStub = null
    }
}