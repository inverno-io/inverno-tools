package test.multi;

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
public abstract class Service1GrpcRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Service1");
	
	private final GrpcServer grpcServer;
	
	public Service1GrpcRoutesConfigurer(GrpcServer grpcServer) {
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
					test.multi.Svc1Request.getDefaultInstance(), 
					test.multi.Svc1Response.getDefaultInstance(), 
					(GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response> grpcExchange) -> this.call(grpcExchange)
				));
	}
	
	/**
	 * 
	 * 
	 * @param exchange the unary exchange
	 */
	public void call(GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::call));
	}

	/**
	 * 
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<test.multi.Svc1Response> call(test.multi.Svc1Request request) {
		throw new UnsupportedOperationException();
	}

}
