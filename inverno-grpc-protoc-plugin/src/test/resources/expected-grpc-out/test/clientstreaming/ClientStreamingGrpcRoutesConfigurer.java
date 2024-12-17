package test.clientstreaming;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a test service with a client streaming method.
 * </p>
 *
 * @param <A> the exchange context type
 */
public abstract class ClientStreamingGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ClientStreaming");
	
	private final GrpcServer grpcServer;
	
	public ClientStreamingGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("callClientStreaming"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.clientStreaming(
					test.clientstreaming.ClientStreamingRequest.getDefaultInstance(), 
					test.clientstreaming.ClientStreamingResponse.getDefaultInstance(), 
					(GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse> grpcExchange) -> this.callClientStreaming(grpcExchange)
				));
	}
	
	/**
	 * <p>
	 * Calls client streaming method.
	 * </p>
	 * 
	 * @param exchange the client streaming exchange
	 */
	public void callClientStreaming(GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse> grpcExchange) {
		grpcExchange.response().value(this.callClientStreaming(grpcExchange.request().stream()));
	}

	/**
	 * <p>
	 * Calls client streaming method.
	 * </p>
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response
	 */
	public Mono<test.clientstreaming.ClientStreamingResponse> callClientStreaming(Publisher<test.clientstreaming.ClientStreamingRequest> request) {
		throw new UnsupportedOperationException();
	}

}
