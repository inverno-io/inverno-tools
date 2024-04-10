package test.bidirectionalstreaming;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import org.reactivestreams.Publisher;

/**
 * <p>
 * This is a test service with a bidirectional streaming method.
 * </p>
 *
 * @param <A> the exchange context type
 */
public abstract class BidirectionalStreamingGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "BidirectionalStreaming");
	
	private final GrpcServer grpcServer;
	
	public BidirectionalStreamingGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRoutable<A, ?> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("callBidirectionalStreaming"))
				.method(Method.POST)
				.consumes(MediaTypes.APPLICATION_GRPC)
				.consumes(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.bidirectionalStreaming(
					test.bidirectionalstreaming.BidirectionalStreamingRequest.getDefaultInstance(), 
					test.bidirectionalstreaming.BidirectionalStreamingResponse.getDefaultInstance(), 
					(GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse> grpcExchange) -> this.callBidirectionalStreaming(grpcExchange)
				));
	}
	
	/**
	 * <p>
	 * Calls bidirectional streaming method.
	 * </p>
	 * 
	 * @param exchange the bidirectional streaming exchange
	 */
	public void callBidirectionalStreaming(GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse> grpcExchange) {
		grpcExchange.response().stream(this.callBidirectionalStreaming(grpcExchange.request().stream()));
	}

	/**
	 * <p>
	 * Calls bidirectional streaming method.
	 * </p>
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response publisher
	 */
	public Publisher<test.bidirectionalstreaming.BidirectionalStreamingResponse> callBidirectionalStreaming(Publisher<test.bidirectionalstreaming.BidirectionalStreamingRequest> request) {
		throw new UnsupportedOperationException();
	}

}
