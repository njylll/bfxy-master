package com.bfxy.payb.service.consumer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bfxy.payb.entity.PlatformAccount;
import com.bfxy.payb.mapper.PlatformAccountMapper;
import com.bfxy.payb.utils.FastJsonConvertUtil;

@Component
public class PayConsumer {
	
	private DefaultMQPushConsumer consumer;
	
	private static final String NAMESERVER = "192.168.11.121:9876;192.168.11.122:9876;192.168.11.123:9876;192.168.11.124:9876";
	
	private static final String CONSUMER_GROUP_NAME = "tx_pay_consumer_group_name";
	
	public static final String TX_PAY_TOPIC = "tx_pay_topic";
	
	public static final String TX_PAY_TAGS = "pay";
	
	@Autowired
	private PlatformAccountMapper platformAccountMapper;
	
	private PayConsumer() {
		try {
			this.consumer = new DefaultMQPushConsumer(CONSUMER_GROUP_NAME);
			this.consumer.setConsumeThreadMin(10);
			this.consumer.setConsumeThreadMax(30);
			this.consumer.setNamesrvAddr(NAMESERVER);
			this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
			this.consumer.subscribe(TX_PAY_TOPIC, TX_PAY_TAGS);
			this.consumer.registerMessageListener(new MessageListenerConcurrently4Pay());
			this.consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
		}
	}
	
	class MessageListenerConcurrently4Pay implements MessageListenerConcurrently {

		@Override
		public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
			MessageExt msg = msgs.get(0);
			try {
				String topic = msg.getTopic();
				String tags = msg.getTags();
				String keys = msg.getKeys();
				String body = new String(msg.getBody(), RemotingHelper.DEFAULT_CHARSET);
				System.err.println("??????????????????, topic: " + topic + ", tags: " + tags + ", keys: " + keys + ", body: " + body);
			
				//	???????????????????????????????????? ???????????????
				//	?????????????????????<????????? keys>
				// 	insert table --> insert ok & primary key
				Map<String, Object> paramsBody = FastJsonConvertUtil.convertJSONToObject(body, Map.class);
				String userId = (String)paramsBody.get("userId");	// customer userId
				String accountId = (String)paramsBody.get("accountId");	//customer accountId
				String orderId = (String)paramsBody.get("orderId");	// 	???????????????
				BigDecimal money = (BigDecimal)paramsBody.get("money");	//	??????????????????
				
				PlatformAccount pa = platformAccountMapper.selectByPrimaryKey("platform001");	//	???????????????????????????
				pa.setCurrentBalance(pa.getCurrentBalance().add(money));
				Date currentTime = new Date();
				pa.setVersion(pa.getVersion() + 1);
				pa.setDateTime(currentTime);
				pa.setUpdateTime(currentTime);
				platformAccountMapper.updateByPrimaryKeySelective(pa);
			} catch (Exception e) {
				e.printStackTrace();
				//msg.getReconsumeTimes();
				//	????????????????????????????????????, ?????????????????????????????? ?????? ???????????????
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
