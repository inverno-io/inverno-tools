package test.reserved;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import reactor.core.publisher.Mono;

/**
 * 
 *
 * @param <A> the exchange context type
 */
public abstract class ReservedKeywordsServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ReservedKeywordsService");
	
	private final GrpcServer grpcServer;
	
	public ReservedKeywordsServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("new"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					test.reserved.ReservedKeywordsRequest.getDefaultInstance(), 
					test.reserved.ReservedKeywordsResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse> grpcExchange) -> this.new_(grpcExchange)
				));
	}
	
	/**
	 * 
	 * 
	 * @param exchange the unary exchange
	 */
	public void new_(GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::new_));
	}

	/**
	 * 
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<test.reserved.ReservedKeywordsResponse> new_(test.reserved.ReservedKeywordsRequest request) {
		throw new UnsupportedOperationException();
	}

}
