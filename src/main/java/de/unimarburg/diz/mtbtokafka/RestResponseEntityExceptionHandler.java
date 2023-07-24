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
import de.unimarburg.diz.mtbtokafka.exceptions.KafkaProduceFailed;
import de.unimarburg.diz.mtbtokafka.exceptions.PseudonymRequestFailed;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central point for error management of REST service
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {JacksonException.class, InterruptedException.class,
        ExecutionException.class, KafkaProduceFailed.class, IllegalArgumentException.class,PseudonymRequestFailed.class})
    protected ResponseEntity<Object> handleInvalidContent(
        Exception ex, WebRequest request) {

        if (ex instanceof JacksonException) {
            return handleMediaNotSupported((JacksonException) ex, request);
        } else if (ex instanceof InterruptedException) {
            return handleInternalError(ex, request);
        } else if (ex instanceof ExecutionException) {
            return handleInternalError(ex, request);
        } else if (ex instanceof IllegalArgumentException) {
            return handleMissingKeyElements(ex, request);
        } else if (ex instanceof KafkaProduceFailed) {
            return handleProduceToKafkaFaild(ex, request);
        } else if (ex instanceof PseudonymRequestFailed) {
            return handleGpasNoConnection(ex, request);
        }
        return handleExceptionInternal(ex, "unknown",
            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> handleGpasNoConnection(Exception ex, WebRequest request) {
        var bodyOfResponse = "Could not process, could not connet to gPas. Check configuration "
            + "'mtb2kafka.pseudonym.gPasUrl' and if gPas can be reached from this instance.";

        return handleExceptionInternal(ex, bodyOfResponse,
            new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    private ResponseEntity<Object> handleProduceToKafkaFaild(Exception ex, WebRequest request) {
        var bodyOfResponse = "Could not process, since Kafka was not reachable or did not accept last method. Try again.";

        return handleExceptionInternal(ex, bodyOfResponse,
            new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    private ResponseEntity<Object> handleMissingKeyElements(Exception ex, WebRequest request) {
        var bodyOfResponse = "MTB file will not be accepted - key elements 'patient id' or 'episode id' are missing.";

        return handleExceptionInternal(ex, bodyOfResponse,
            new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Nullable
    private ResponseEntity<Object> handleMediaNotSupported(JacksonException ex,
        WebRequest request) {
        var bodyOfResponse = "Please check sent document. "
            + "It may be invalid JSON or identifying MTB properties are missing.";

        return handleExceptionInternal(ex, bodyOfResponse,
            new HttpHeaders(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }


    protected ResponseEntity<Object> handleInternalError(
        Exception ex, WebRequest request) {

        var bodyOfResponse = "Internal error occurred or Kafka is currently not available. "
            + "Date processing failed. If this persists, please contact support.";

        var header = new HttpHeaders();
        header.add(HttpHeaders.RETRY_AFTER, "30");

        return handleExceptionInternal(ex, bodyOfResponse,
            header, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

}
