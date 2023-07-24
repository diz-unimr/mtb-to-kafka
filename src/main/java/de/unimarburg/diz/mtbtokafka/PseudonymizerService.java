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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unimarburg.diz.mtbtokafka.exceptions.PseudonymRequestFailed;
import java.net.ConnectException;
import java.util.HashMap;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(value = "mtb2kafka.pseudonym.enabled")
public class PseudonymizerService {

    private final Logger log = LoggerFactory.getLogger(PseudonymizerService.class);

    private final String gPasUrl;
    private final String psnTargetDomain;
    private final RetryTemplate retryTemplate = defaultTemplate();

    private final FhirContext r4Context;

    public PseudonymizerService(@Value("${mtb2kafka.pseudonym.gPasUrl}") String gPasBaseUrl,
        @Value("${mtb2kafka.pseudonym.target}") String psnTargetDomain) {

        this.gPasUrl = gPasBaseUrl + "/ttp-fhir/fhir/gpas/$pseudonymizeAllowCreate";
        this.psnTargetDomain = psnTargetDomain;
        this.r4Context = FhirContext.forR4();
    }

    public Parameters getPseudonymParameters(Parameters pseudonymParameters) {

        final IParser iParser = r4Context.newJsonParser();
        String message = iParser.encodeResourceToString(pseudonymParameters);
        ResponseEntity<String> responseEntity = performRestCallToGpas(message);

        return (Parameters) iParser.parseResource(responseEntity.getBody());
    }

    @NotNull
    protected ResponseEntity<String> performRestCallToGpas(String message) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(message, headers);
        ResponseEntity<String> responseEntity;

        try {
            responseEntity = retryTemplate.execute(
                ctx -> restTemplate.exchange(gPasUrl, HttpMethod.POST, requestEntity,
                    String.class));

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                log.debug("API request succeeded. Response: {}", responseEntity.getStatusCode());
            } else {
                log.warn("API request unsuccessful. Response: {}", requestEntity.getBody());
                throw new PseudonymRequestFailed("API request unsuccessful gPas unsuccessful.");
            }

            return responseEntity;
        } catch (Exception unexpected) {
            throw new PseudonymRequestFailed(
                "API request due unexpected error unsuccessful gPas unsuccessful.", unexpected);
        }
    }

    protected RetryTemplate defaultTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(1.25);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        HashMap<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        retryableExceptions.put(ConnectException.class, true);
        RetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context,
                RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("HTTP Error occurred: {}. Retrying {}", throwable.getMessage(),
                    context.getRetryCount());
            }
        });

        return retryTemplate;
    }

    public void pseudonymizeMtb(JsonNode jsonNode) {
        final JsonNode patientNode = jsonNode.get("patient");
        final JsonNode patientId = patientNode.get("id");

        var requestParameters = new Parameters();
        requestParameters.addParameter().setName("target")
            .setValue(new StringType().setValue(psnTargetDomain));
        requestParameters.addParameter().setName("original")
            .setValue(new StringType().setValue(patientId.asText()));

        var pseudonymParameter = getPseudonymParameters(requestParameters);

        // todo: error handling
        Identifier pseudonym = (Identifier) pseudonymParameter.getParameter().stream().findFirst()
            .get().getPart().stream().filter(a -> a.getName().equals("pseudonym")).findFirst()
            .orElseGet(
                ParametersParameterComponent::new).getValue();

        // pseudonym
        var pidAsPseudonym = pseudonym.getSystem() + "|" + pseudonym.getValue();

        // replace PID with pseudonym
        ((ObjectNode) patientNode).put("id", pidAsPseudonym);

        // TODO: replace other ID properties with hashed values
    }
}
