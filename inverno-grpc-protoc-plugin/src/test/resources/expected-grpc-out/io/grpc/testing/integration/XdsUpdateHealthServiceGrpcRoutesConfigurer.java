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
 * A service to remotely control health status of an xDS test server.
 *
 * @param <A> the exchange context type
 */
public abstract class XdsUpdateHealthServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "XdsUpdateHealthService");
	
	private final GrpcServer grpcServer;
	
	public XdsUpdateHealthServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("SetServing"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) -> this.setServing(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("SetNotServing"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) -> this.setNotServing(grpcExchange)
				));
	}
	
	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void setServing(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::setServing));
	}

	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.EmptyProtos.Empty> setServing(io.grpc.testing.integration.EmptyProtos.Empty request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void setNotServing(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::setNotServing));
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.EmptyProtos.Empty> setNotServing(io.grpc.testing.integration.EmptyProtos.Empty request) {
		throw new UnsupportedOperationException();
	}

}
