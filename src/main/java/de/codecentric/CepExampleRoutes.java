package de.codecentric;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.espertech.esper.event.map.MapEventBean;

/**
 * 
 * Example routes to consume Events.
 * 
 */
@Component
public class CepExampleRoutes extends RouteBuilder {

	Logger LOG = LoggerFactory.getLogger(CepExampleRoutes.class);

	@Override
	public void configure() throws Exception {

		from("esper://esper-dom?eql=select count(*) as cnt from de.codecentric.CartCreatedEvent.win:time(5 sec) where totalAmount > 200")
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						MapEventBean event = (MapEventBean) exchange.getIn().getBody(MapEventBean.class);
						LOG.info("Current count of records: {} > 200", event.get("cnt"));
					}
				});

		from("esper://esper-dom?eql=select avg(totalAmount) as avg from de.codecentric.CartCreatedEvent.win:time(5 sec)")
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						MapEventBean event = (MapEventBean) exchange.getIn().getBody(MapEventBean.class);
						LOG.info("Current average ammount: {}", event.get("avg"));
					}
				});

		from("esper://esper-dom?eql=select billingCountry, totalAmount from de.codecentric.CartCreatedEvent.win:time_batch(5 sec) where billingCountry not in ('Germany')")
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						MapEventBean event = (MapEventBean) exchange.getIn().getBody(MapEventBean.class);
						LOG.info("have to write an invoice with taxes vor country: {}", event.get("billingCountry"));
						int totalAmount = (int) event.get("totalAmount");
						
						exchange.getIn().setBody(new ForeignInvoiceCreatedEvent(totalAmount, 
								(String) event.get("billingCountry")));
					}
				}).marshal().json().convertBodyTo(String.class).setHeader(KafkaConstants.PARTITION_KEY)
				.simple(".").setHeader(KafkaConstants.KEY).simple("1")
				.to("kafka:localhost:9092?topic=eventChannel&serializerClass=kafka.serializer.StringEncoder");

	}

}
