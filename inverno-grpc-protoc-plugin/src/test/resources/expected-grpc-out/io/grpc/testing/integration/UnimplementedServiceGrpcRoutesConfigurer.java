package io.grpc.testing.integration;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import reactor.core.publisher.Mono;

/**
 * A simple service NOT implemented at servers so clients can test for
 * that case.
 *
 * @param <A> the exchange context type
 */
public abstract class UnimplementedServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "UnimplementedService");
	
	private final GrpcServer grpcServer;
	
	public UnimplementedServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("UnimplementedCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) -> this.unimplementedCall(grpcExchange)
				));
	}
	
	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void unimplementedCall(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::unimplementedCall));
	}

	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
		throw new UnsupportedOperationException();
	}

}
