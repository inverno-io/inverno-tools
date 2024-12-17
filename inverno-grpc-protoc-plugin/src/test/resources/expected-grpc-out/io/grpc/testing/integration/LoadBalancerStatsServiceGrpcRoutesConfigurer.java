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
 * A service used to obtain stats for verifying LB behavior.
 *
 * @param <A> the exchange context type
 */
public abstract class LoadBalancerStatsServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "LoadBalancerStatsService");
	
	private final GrpcServer grpcServer;
	
	public LoadBalancerStatsServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("GetClientStats"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.Messages.LoadBalancerStatsRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.LoadBalancerStatsResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> grpcExchange) -> this.getClientStats(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("GetClientAccumulatedStats"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> grpcExchange) -> this.getClientAccumulatedStats(grpcExchange)
				));
	}
	
	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void getClientStats(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::getClientStats));
	}

	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> getClientStats(io.grpc.testing.integration.Messages.LoadBalancerStatsRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void getClientAccumulatedStats(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::getClientAccumulatedStats));
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> getClientAccumulatedStats(io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest request) {
		throw new UnsupportedOperationException();
	}

}
