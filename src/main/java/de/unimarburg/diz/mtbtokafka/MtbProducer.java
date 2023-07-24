/*
 This file is part of MTB-TO-KAFKA.

 MTB-TO-KAFKA - Input MTB file via REST > gPas pseudonym > produce to Apache Kafka topic
 Copyright (C) 2023  Datenintegrationszentrum Philipps-Universität Marburg

 MTB-TO-KAFKA is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.

 MTB-TO-KAFKA is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package de.unimarburg.diz.mtbtokafka;

import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;


@Configuration
public class MtbProducer {

    protected final KafkaTemplate<String, String> kafkaTemplate;
    Logger log = LoggerFactory.getLogger(MtbProducer.class);

    public MtbProducer(KafkaTemplate<String, String> kafkaTemplate,
        @Value("${mtb2kafka.mtbProducerOutput.destination}") String defaultTopicName) {

        this.kafkaTemplate = kafkaTemplate;

        kafkaTemplate.setDefaultTopic(defaultTopicName);
    }

    public boolean sendToKafka(String key, String data)
        throws InterruptedException, ExecutionException {
        var result = kafkaTemplate.sendDefault(key, data);

        if (result.get() != null) {
            log.debug("stored msg : " + data);
        } else {
            log.error("failed! send data: " + data);
            return false;
        }

        return true;
    }

}
