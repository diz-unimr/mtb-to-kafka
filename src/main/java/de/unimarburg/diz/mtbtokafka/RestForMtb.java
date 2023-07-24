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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.unimarburg.diz.mtbtokafka.exceptions.KafkaProduceFailed;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO: handle DELETE
 */
@RestController
public class RestForMtb {

    private final PseudonymizerService pseudonymizerService;
    protected MtbProducer mtbProducer;
    private final Logger log = LoggerFactory.getLogger(RestForMtb.class);
    private final ObjectMapper objectMapper;


    @Autowired
    public RestForMtb(MtbProducer mtbProducer, Optional<PseudonymizerService> pseudonymService) {
        this.mtbProducer = mtbProducer;
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.pseudonymizerService = pseudonymService.orElse(null);
    }


    @DeleteMapping("/mtbfile")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void deleteMtbFile() {
        // TODO
        // throw new NotImplementedException("currently not implemented!");
    }

    /**
     * Main entry for MTB file processing
     *
     * @param newMtbFile input data
     * @throws JacksonException     if input is invalid json
     * @throws ExecutionException   produce data into kafka failed
     * @throws InterruptedException produce data into kafka failed
     * @throws KafkaProduceFailed   if produce to Kafka fails
     */
    @PostMapping("/mtbfile")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void acceptMtbFile(@RequestBody String newMtbFile)
        throws JacksonException, ExecutionException, InterruptedException, KafkaProduceFailed {
        processMtbFile(newMtbFile);

    }

    protected boolean processMtbFile(String data)
        throws JacksonException, InterruptedException, ExecutionException, KafkaProduceFailed {
        try {
            var jsonNode = objectMapper.readTree(data);

            if (pseudonymizerService != null) {
                pseudonymizerService.pseudonymizeMtb(jsonNode);
            }

            final String key = getKey(jsonNode);

            var kafkaHasAccepted = mtbProducer.sendToKafka(key,
                objectMapper.writeValueAsString(jsonNode));

            if (!kafkaHasAccepted) {
                throw new KafkaProduceFailed("Kafka processing failed");
            }
        } catch (JacksonException jsonException) {
            log.error("JSON parsing failed. Please check file generation process.", jsonException);
            throw jsonException;
        } catch (InterruptedException | ExecutionException e) {
            // Handle serialization errors
            log.error("failed send data to kafka", e);
            throw e;
        }
        return true;
    }

    @NotNull
    private String getKey(JsonNode jsonNode) throws JsonProcessingException {

        if (!jsonNode.hasNonNull("patient") || !jsonNode.hasNonNull("episode")) {
            throw new IllegalArgumentException(
                "patient and episode id must be provided. rejecting current data!");
        }

        var patientId = jsonNode.get("patient").get("id");
        var episodeId = jsonNode.get("episode").get("id");

        return String.format("{\"pid\": %s, \"eid\":%s}", patientId, episodeId);
    }

}
