package test.serverstreaming;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import org.reactivestreams.Publisher;

/**
 * <p>
 * This is a test service with a server streaming method.
 * </p>
 *
 * @param <A> the exchange context type
 */
public abstract class ServerStreamingGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ServerStreaming");
	
	private final GrpcServer grpcServer;
	
	public ServerStreamingGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("callServerStreaming"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.serverStreaming(
					test.serverstreaming.ServerStreamingRequest.getDefaultInstance(), 
					test.serverstreaming.ServerStreamingResponse.getDefaultInstance(), 
					(GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse> grpcExchange) -> this.callServerStreaming(grpcExchange)
				));
	}
	
	/**
	 * <p>
	 * Calls server streaming method.
	 * </p>
	 * 
	 * @param exchange the server streaming exchange
	 */
	public void callServerStreaming(GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse> grpcExchange) {
		grpcExchange.response().stream(grpcExchange.request().value().flatMapMany(this::callServerStreaming));
	}

	/**
	 * <p>
	 * Calls server streaming method.
	 * </p>
	 * 
	 * @param request the client request
	 * 
	 * @return the server response publisher
	 */
	public Publisher<test.serverstreaming.ServerStreamingResponse> callServerStreaming(test.serverstreaming.ServerStreamingRequest request) {
		throw new UnsupportedOperationException();
	}

}
