package xyz.jdubiel.migawka.data.network

import io.grpc.CallCredentials
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import xyz.jdubiel.migawka.MigawkaGrpcKt
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class BearerTokenCredentials(private val token: String) : CallCredentials() {
    override fun applyRequestMetadata(requestInfo: RequestInfo, appExecutor: Executor, applier: MetadataApplier) {
        appExecutor.execute {
            try {
                val headers = Metadata()
                val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                headers.put(authKey, "Bearer $token")
                applier.apply(headers)
            } catch (e: Throwable) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e))
            }
        }
    }
}

class GrpcProvider(private val remoteEndpoint: IPEndpoint) {
    private var channel: ManagedChannel? = null
    private var migawkaServiceStub: MigawkaGrpcKt.MigawkaCoroutineStub? = null

    @Synchronized
    fun getChannel(): ManagedChannel {
        if (channel == null) {
            channel = ManagedChannelBuilder
                .forAddress(remoteEndpoint.ip, remoteEndpoint.port)
                .useTransportSecurity()
                .build()
        }
        return channel!!
    }

    @Synchronized
    fun getMigawkaServiceStub(): MigawkaGrpcKt.MigawkaCoroutineStub {
        if (migawkaServiceStub == null) {
            migawkaServiceStub = MigawkaGrpcKt.MigawkaCoroutineStub(getChannel())
                .withCallCredentials(BearerTokenCredentials("my-secret-token"))
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