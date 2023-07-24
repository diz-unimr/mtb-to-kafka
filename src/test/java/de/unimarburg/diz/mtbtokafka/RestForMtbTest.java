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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JacksonException;
import de.unimarburg.diz.mtbtokafka.exceptions.KafkaProduceFailed;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@AutoConfigureMockMvc
@SpringBootTest(classes = {RestForMtb.class, RestTemplate.class,
    RestResponseEntityExceptionHandler.class})
public class RestForMtbTest {

    public final static String MTB_FILE_DUMMY = """
        {
        	"patient": {
        		"id": "fc7e5ddc-c70d-45fd-8cf5-799d50871ce9",
        		"gender": "male",
        		"birthDate": "1975-01",
        		"insurance": "Barmer"
        	},
        	"consent": {
        		"id": "e53ee7ba-a8e8-4262-8f7d-f80a81f31dc0",
        		"patient": "fc7e5ddc-c70d-45fd-8cf5-799d50871ce9",
        		"status": "active"
        	},
        	"episode": {
        		"id": "5e28e38d-e172-43a9-bcde-257c5f7970d9",
        		"patient": "fc7e5ddc-c70d-45fd-8cf5-799d50871ce9",
        		"period": {
        			"start": "2023-06-29"
        		}
        	}
        }
        """;
    public final static String MTB_FILE_DUMMY_KEY = "fc7e5ddc-c70d-45fd-8cf5-799d50871ce9-5e28e38d-e172-43a9-bcde-257c5f7970d9";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    MtbProducer producer;

    @Autowired
    private RestForMtb fixture;

    @Test
    public void processMtbFile_valid_input_return_true()
        throws JacksonException, ExecutionException, InterruptedException, KafkaProduceFailed {
        given(producer.sendToKafka(anyString(), anyString())).willReturn(true);
        assertThat(fixture.processMtbFile(MTB_FILE_DUMMY)).isTrue();
    }


    @Test
    public void processMtbFile_invalid_json_exceptionThrown() {
        final String testInput = "{\"test\": \"input\"";
        assertThrows(JacksonException.class, () -> fixture.processMtbFile(testInput));
    }

    @Test
    public void rest_call_accepted() throws Exception {
        given(producer.sendToKafka(anyString(), anyString())).willReturn(true);
        var result = mockMvc.perform(
                post("/mtbfile").content(MTB_FILE_DUMMY).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isAccepted())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }


    @Test
    public void rest_call_invalid_file() throws Exception {
        final String bodyContent = "{\"test\": \"input\"";
        var result = mockMvc.perform(
                post("/mtbfile").content(bodyContent).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnsupportedMediaType())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }

    @Test
    public void rest_call_missing_properties() throws Exception {
        final String bodyContent = "{\"test\": \"input\"}";
        var result = mockMvc.perform(
                post("/mtbfile").content(bodyContent).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }

    @Test
    public void rest_call_kafka_produce_faild() throws Exception {
        given(producer.sendToKafka(anyString(), anyString())).willReturn(false);
        var result = mockMvc.perform(
                post("/mtbfile").content(MTB_FILE_DUMMY).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable())
            .andReturn();
    }
}
