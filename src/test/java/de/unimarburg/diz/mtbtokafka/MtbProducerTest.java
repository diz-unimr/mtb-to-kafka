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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

public class MtbProducerTest {


    @Test
    public void sendJsonMessageTest()
        throws ExecutionException, InterruptedException {

        final String dummyTopicName = "dummyTopicName";

        var kafkaTemplateMocked = Mockito.mock(KafkaTemplate.class);
        var futureMocked = Mockito.mock(java.util.concurrent.CompletableFuture.class);

        Mockito.when(futureMocked.get()).thenReturn("unitTest");
        Mockito.when(kafkaTemplateMocked.sendDefault(RestForMtbTest.MTB_FILE_DUMMY_KEY,
            RestForMtbTest.MTB_FILE_DUMMY)).thenReturn(futureMocked);

        var fixture = new MtbProducer(kafkaTemplateMocked,  dummyTopicName);

        fixture.sendToKafka(RestForMtbTest.MTB_FILE_DUMMY_KEY, RestForMtbTest.MTB_FILE_DUMMY);
        Mockito.verify(kafkaTemplateMocked)
            .sendDefault(RestForMtbTest.MTB_FILE_DUMMY_KEY, RestForMtbTest.MTB_FILE_DUMMY);
    }
}
