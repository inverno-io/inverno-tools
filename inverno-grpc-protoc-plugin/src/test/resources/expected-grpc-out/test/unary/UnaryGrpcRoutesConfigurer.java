package test.unary;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a test service with a unary method.
 * </p>
 *
 * @param <A> the exchange context type
 */
public abstract class UnaryGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Unary");
	
	private final GrpcServer grpcServer;
	
	public UnaryGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("callUnary"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					test.unary.UnaryRequest.getDefaultInstance(), 
					test.unary.UnaryResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse> grpcExchange) -> this.callUnary(grpcExchange)
				));
	}
	
	/**
	 * <p>
	 * Calls unary method.
	 * </p>
	 * 
	 * @param exchange the unary exchange
	 */
	public void callUnary(GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::callUnary));
	}

	/**
	 * <p>
	 * Calls unary method.
	 * </p>
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<test.unary.UnaryResponse> callUnary(test.unary.UnaryRequest request) {
		throw new UnsupportedOperationException();
	}

}
