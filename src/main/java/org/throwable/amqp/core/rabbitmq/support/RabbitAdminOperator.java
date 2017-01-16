package org.throwable.amqp.core.rabbitmq.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.util.Assert;
import org.throwable.amqp.core.rabbitmq.constants.ExchangeEnum;
import org.throwable.amqp.core.rabbitmq.entity.BindingParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangjinci
 * @version 2017/1/16 16:15
 * @function
 */

public class RabbitAdminOperator {

    private static final Logger logger = LoggerFactory.getLogger(RabbitAdminOperator.class);

    private RabbitAdmin rabbitAdmin;

    public RabbitAdminOperator(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void init(List<BindingParameters> bindingParameters) {
        List<Queue> queues = buildQueues(bindingParameters);
        for (Queue queue : queues) {
            logger.debug("Rabbitmq AmqpAdmin declareQueue :" + queue.getName());
            rabbitAdmin.declareQueue(queue);
        }
        List<Exchange> exchanges = buildExchanges(bindingParameters);
        for (Exchange exchange : exchanges) {
            logger.debug("Rabbitmq AmqpAdmin declareExchange :" + exchange.getName());
            rabbitAdmin.declareExchange(exchange);
        }
        List<Binding> bindings = buildBindings(bindingParameters);
        for (Binding binding : bindings) {
            logger.debug("Rabbitmq AmqpAdmin declareBinding :" + binding.getDestination());
            rabbitAdmin.declareBinding(binding);
        }
    }

    private List<Queue> buildQueues(List<BindingParameters> bindingParameters) {
        List<Queue> queues = new ArrayList<>(bindingParameters.size());
        for (BindingParameters queue : bindingParameters) {
            queues.add(createQueue(queue.getQueueName()));
        }
        return queues;
    }

    /**
     * 构造队列对象,默认持久化
     *
     * @param name name
     * @return Queue
     */
    private Queue createQueue(String name) {
        return new Queue(name, true);
    }


    public List<Binding> buildBindings(List<BindingParameters> bindingParameters) {
        List<Binding> bindings = new ArrayList<>(bindingParameters.size());
        for (BindingParameters bindingSingle : bindingParameters) {
            bindings.add(transferBinding(bindingSingle));
        }
        return bindings;
    }

    /**
     * 根据Exchange类型进行Binding装换
     *
     * @param bindingSingle bindingSingle
     * @return Binding
     */
    private Binding transferBinding(BindingParameters bindingSingle) {
        Binding binding;
        String type = bindingSingle.getExchangeType().toUpperCase();
        Assert.hasText(type);
        switch (ExchangeEnum.valueOf(type)) {
            case DIRECT:
                binding = BindingBuilder.bind(createQueue(bindingSingle.getQueueName()))
                        .to(new DirectExchange(bindingSingle.getExchangeName()))
                        .with(bindingSingle.getRoutingKey());
                break;
            case TOPIC:
                binding = BindingBuilder.bind(createQueue(bindingSingle.getQueueName()))
                        .to(new TopicExchange(bindingSingle.getExchangeName()))
                        .with(bindingSingle.getRoutingKey());
                break;
            case HEADERS:
                binding = BindingBuilder.bind(createQueue(bindingSingle.getQueueName()))
                        .to(new HeadersExchange(bindingSingle.getExchangeName()))
                        .whereAll(exchangeHeaderToMap(bindingSingle.getRoutingKey()))
                        .match();
                break;
            case FANOUT:
                binding = BindingBuilder.bind(createQueue(bindingSingle.getQueueName()))
                        .to(new FanoutExchange(bindingSingle.getExchangeName()));
                break;
            default: {
                binding = BindingBuilder.bind(createQueue(bindingSingle.getQueueName()))
                        .to(createExchange(bindingSingle.getExchangeName(), bindingSingle.getExchangeType()))
                        .with(bindingSingle.getRoutingKey()).noargs();
            }
        }
        return binding;
    }

    private Map<String, Object> exchangeHeaderToMap(String key) {
        String[] mapEntrys = key.split(";");
        Map<String, Object> result = new HashMap<>(mapEntrys.length);
        for (String mapEntry : mapEntrys) {
            String[] entry = mapEntry.split("=");
            result.put(entry[0], entry[1]);
        }
        return result;
    }


    public List<Exchange> buildExchanges(List<BindingParameters> bindingParameters) {
        List<Exchange> exchanges = new ArrayList<>(bindingParameters.size());
        for (BindingParameters exchange : bindingParameters) {
            exchanges.add(createExchange(exchange.getExchangeName(), exchange.getExchangeType()));
        }
        return exchanges;
    }

    /**
     * 构造交换器对象,默认持久化
     *
     * @param name name
     * @return Exchange
     */
    private Exchange createExchange(String name, String type) {
        return transferExchanges(name, type);
    }


    private Exchange transferExchanges(String name, String type) {
        Exchange exchange;
        String typeName = type.toUpperCase();
        Assert.hasText(typeName);
        switch (ExchangeEnum.valueOf(typeName)) {
            case DIRECT:
                exchange = new DirectExchange(name);
                break;
            case TOPIC:
                exchange = new TopicExchange(name);
                break;
            case HEADERS:
                exchange = new HeadersExchange(name);
                break;
            case FANOUT:
                exchange = new FanoutExchange(name);
                break;
            default: {
                exchange = new CustomExchange(name, type, true, false);
            }
        }
        return exchange;
    }

}