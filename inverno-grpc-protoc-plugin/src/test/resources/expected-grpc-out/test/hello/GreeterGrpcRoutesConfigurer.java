package test.hello;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a hello service.
 * </p>
 * 
 * <p>
 * It is called Greeter and has 4 methods:
 * </p>
 * 
 * <ul>
 *   <li>SayHelloUnary</li>
 *   <li>SayHelloClientStreaming</li>
 *   <li>SayHelloServerStreaming</li>
 *   <li>SayHelloBidirectionalStreaming</li>
 * </ul>
 *
 * @param <A> the exchange context type
 */
public abstract class GreeterGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Greeter");
	
	private final GrpcServer grpcServer;
	
	public GreeterGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("SayHelloUnary"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					test.hello.HelloRequest.getDefaultInstance(), 
					test.hello.HelloResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) -> this.sayHelloUnary(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("SayHelloClientStreaming"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.clientStreaming(
					test.hello.HelloRequest.getDefaultInstance(), 
					test.hello.HelloResponse.getDefaultInstance(), 
					(GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) -> this.sayHelloClientStreaming(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("SayHelloServerStreaming"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.serverStreaming(
					test.hello.HelloRequest.getDefaultInstance(), 
					test.hello.HelloResponse.getDefaultInstance(), 
					(GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) -> this.sayHelloServerStreaming(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("SayHelloBidirectionalStreaming"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.bidirectionalStreaming(
					test.hello.HelloRequest.getDefaultInstance(), 
					test.hello.HelloResponse.getDefaultInstance(), 
					(GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) -> this.sayHelloBidirectionalStreaming(grpcExchange)
				));
	}
	
	/**
	 * <p>
	 * Unary hello request.
	 * </p>
	 * 
	 * @param exchange the unary exchange
	 */
	public void sayHelloUnary(GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::sayHelloUnary));
	}

	/**
	 * <p>
	 * Unary hello request.
	 * </p>
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<test.hello.HelloResponse> sayHelloUnary(test.hello.HelloRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>
	 * Client Streaming hello request.
	 * </p>
	 * 
	 * @param exchange the client streaming exchange
	 */
	public void sayHelloClientStreaming(GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) {
		grpcExchange.response().value(this.sayHelloClientStreaming(grpcExchange.request().stream()));
	}

	/**
	 * <p>
	 * Client Streaming hello request.
	 * </p>
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response
	 */
	public Mono<test.hello.HelloResponse> sayHelloClientStreaming(Publisher<test.hello.HelloRequest> request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>
	 * Server Streaming hello request.
	 * </p>
	 * 
	 * @param exchange the server streaming exchange
	 */
	public void sayHelloServerStreaming(GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) {
		grpcExchange.response().stream(grpcExchange.request().value().flatMapMany(this::sayHelloServerStreaming));
	}

	/**
	 * <p>
	 * Server Streaming hello request.
	 * </p>
	 * 
	 * @param request the client request
	 * 
	 * @return the server response publisher
	 */
	public Publisher<test.hello.HelloResponse> sayHelloServerStreaming(test.hello.HelloRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>
	 * Birirectional Streaming hello request.
	 * </p>
	 * 
	 * @param exchange the bidirectional streaming exchange
	 */
	public void sayHelloBidirectionalStreaming(GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange) {
		grpcExchange.response().stream(this.sayHelloBidirectionalStreaming(grpcExchange.request().stream()));
	}

	/**
	 * <p>
	 * Birirectional Streaming hello request.
	 * </p>
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response publisher
	 */
	public Publisher<test.hello.HelloResponse> sayHelloBidirectionalStreaming(Publisher<test.hello.HelloRequest> request) {
		throw new UnsupportedOperationException();
	}

}
