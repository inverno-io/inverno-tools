package test.multi;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * 
 *
 * @param <A> the exchange context type
 */
public abstract class Service2GrpcRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Service2");
	
	private final GrpcServer grpcServer;
	
	public Service2GrpcRoutesConfigurer(GrpcServer grpcServer) {
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
				.handler(this.grpcServer.clientStreaming(
					test.multi.Svc2Request.getDefaultInstance(), 
					test.multi.Svc2Response.getDefaultInstance(), 
					(GrpcExchange.ClientStreaming<A, test.multi.Svc2Request, test.multi.Svc2Response> grpcExchange) -> this.call(grpcExchange)
				));
	}
	
	/**
	 * 
	 * 
	 * @param exchange the client streaming exchange
	 */
	public void call(GrpcExchange.ClientStreaming<A, test.multi.Svc2Request, test.multi.Svc2Response> grpcExchange) {
		grpcExchange.response().value(this.call(grpcExchange.request().stream()));
	}

	/**
	 * 
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response
	 */
	public Mono<test.multi.Svc2Response> call(Publisher<test.multi.Svc2Request> request) {
		throw new UnsupportedOperationException();
	}

}
