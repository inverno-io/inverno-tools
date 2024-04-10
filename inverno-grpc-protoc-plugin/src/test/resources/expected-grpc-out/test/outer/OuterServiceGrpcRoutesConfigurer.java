package test.outer;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import reactor.core.publisher.Mono;

/**
 * 
 *
 * @param <A> the exchange context type
 */
public abstract class OuterServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "OuterService");
	
	private final GrpcServer grpcServer;
	
	public OuterServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRoutable<A, ?> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("call"))
				.method(Method.POST)
				.consumes(MediaTypes.APPLICATION_GRPC)
				.consumes(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					test.outer.OuterClass.OuterRequest.getDefaultInstance(), 
					test.outer.OuterClass.OuterResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse> grpcExchange) -> this.call(grpcExchange)
				));
	}
	
	/**
	 * 
	 * 
	 * @param exchange the unary exchange
	 */
	public void call(GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::call));
	}

	/**
	 * 
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<test.outer.OuterClass.OuterResponse> call(test.outer.OuterClass.OuterRequest request) {
		throw new UnsupportedOperationException();
	}

}
