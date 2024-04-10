package io.grpc.testing.integration;

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
 * A service to dynamically update the configuration of an xDS test client.
 *
 * @param <A> the exchange context type
 */
public abstract class XdsUpdateClientConfigureServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "XdsUpdateClientConfigureService");
	
	private final GrpcServer grpcServer;
	
	public XdsUpdateClientConfigureServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRoutable<A, ?> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("Configure"))
				.method(Method.POST)
				.consumes(MediaTypes.APPLICATION_GRPC)
				.consumes(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.Messages.ClientConfigureRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.ClientConfigureResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse> grpcExchange) -> this.configure(grpcExchange)
				));
	}
	
	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void configure(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::configure));
	}

	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request) {
		throw new UnsupportedOperationException();
	}

}
