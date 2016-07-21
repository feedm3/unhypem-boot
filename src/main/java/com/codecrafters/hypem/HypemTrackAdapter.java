package com.codecrafters.hypem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

/**
 * This class is used to get the hosting URL of a song on hypem. All you need is the hypem id of the song (the id is
 * also used in the URL of the song).
 *
 * @author Fabian Dietenberger
 */
public class HypemTrackAdapter {

    private static final String SOUNDCLOUD_HOST_NAME = "soundcloud.com";

    // all requests need the same cookie because hypem will generate a key with it
    private static final String HYPEM_AUTH_COOKIE = "AUTH=03:95e416f279a4f69d206c4786c7fb3fd6:1435915799:1527019462:10-DE";

    private static final String HYPEM_TRACK_URL = "http://hypem.com/track/";
    private static final String HYPEM_GO_URL = "http://hypem.com/go/sc/";
    private static final String HYPEM_SERVE_URL = "http://hypem.com/serve/source/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HypemTrackAdapter(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        setupRestErrorHandler();
    }

    /**
     * Get the hypem id from a hypem track URL. This function simply extracts the id from the URL.
     * <p>
     * Example: http://hypem.com/track/2c87x will return 2c87x
     *
     * @param hypemTrackUrl the hypem URL to a song
     * @return the media id from the URL or an empty string if the URL is not valid
     */
    public String getHypemMediaIdFromUrl(final String hypemTrackUrl) {
        final String trimmedUrl = StringUtils.trim(hypemTrackUrl);
        if (StringUtils.startsWith(trimmedUrl, HYPEM_TRACK_URL)) {
            return extractIdFromHypemUrl(trimmedUrl);
        }
        return "";
    }

    /**
     * Get the songs hosting URL from a hypem id. Most of the times it's a soundcloud or mp3 URL.
     *
     * @param hypemId the hypem id to resolve the hosting URL
     * @return the URL to the song or null if the id cannot be resolved
     */
    public URI getFileUriByHypemId(final String hypemId) {
        if (StringUtils.isNotBlank(hypemId)) {
            final URI goUrl = getHostingGoUrl(hypemId);
            if (isSoundcloudUrl(goUrl)) {
                return goUrl;
            } else {
                // if the song is not hosted on soundcloud we need to request another hypem endpoint
                final String jsonBody = getHostingServeJsonBody(hypemId);
                if (StringUtils.isNotBlank(jsonBody)) {
                    return extractUrlField(jsonBody);
                }
            }
        }
        return null;
    }

    private String extractIdFromHypemUrl(final String hypemTrackUrl) {
        final URI trackUri = URI.create(hypemTrackUrl);
        final String[] pathParts = trackUri.getPath().split("/");
        if (pathParts.length >= 3) {
            // looks like "/track/id"
            return pathParts[2];
        }
        return "";
    }

    private URI getHostingGoUrl(final String hypemId) {
        final RequestEntity<Void> requestEntity = RequestEntity.head(URI.create(HYPEM_GO_URL + hypemId)).build();
        final ResponseEntity<Void> exchange = restTemplate.exchange(requestEntity, Void.class);
        return exchange.getHeaders().getLocation();
    }

    private String getHostingServeJsonBody(final String hypemId) {
        final String key = getTrackUrlAccessKey(hypemId);
        final RequestEntity<Void> mp3Request = RequestEntity.get(URI.create(HYPEM_SERVE_URL + hypemId + "/" + key)).header("Cookie", HYPEM_AUTH_COOKIE).build();
        final ResponseEntity<String> mp3Response = restTemplate.exchange(mp3Request, String.class);

        if (mp3Response.getStatusCode() == HttpStatus.OK) {
            return mp3Response.getBody();
        }
        return "";
    }

    private URI extractUrlField(final String json) {
        try {
            final JsonNode mp3ResponseJsonNode = objectMapper.readTree(json);
            final String url = mp3ResponseJsonNode.get("url").asText();
            return URI.create(url);
        } catch (IOException e) {
            // if this exception occurs we have to improve the - vary naive - parsing
            e.printStackTrace();
            return null;
        }
    }

    /**
     * To get the MP3 URL of a song we first need a key which we can later use
     * to access the URL. The key is inside a response from hypem so we have to
     * parse through this response to find the kay attribute.
     *
     * @param hypemId the hypem id to get the key
     * @return the key
     */
    private String getTrackUrlAccessKey(final String hypemId) {
        final RequestEntity<Void> hypemKeyRequest = RequestEntity.get(URI.create(HYPEM_TRACK_URL + hypemId)).header("Cookie", HYPEM_AUTH_COOKIE).build();
        final ResponseEntity<String> hypemKeyResponse = restTemplate.exchange(hypemKeyRequest, String.class);

        final String body = hypemKeyResponse.getBody();
        final String rawSongJson = StringUtils.substringBetween(body, "<script type=\"application/json\" id=\"displayList-data\">", "<script type=\"text/javascript\">");
        final String songJson = StringUtils.trim(rawSongJson);

        try {
            final JsonNode jsonNode = objectMapper.readTree(songJson);
            return jsonNode.get("tracks").get(0).get("key").asText();
        } catch (IOException e) {
            // if this exception occurs we have to improve the - vary naive - parsing
            e.printStackTrace();
            return "";
        }
    }

    private boolean isSoundcloudUrl(final URI fileUri) {
        return fileUri != null &&
                StringUtils.equals(fileUri.getHost(), SOUNDCLOUD_HOST_NAME) &&
                !StringUtils.equals(fileUri.getPath(), "/not/found");
    }

    private void setupRestErrorHandler() {
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(final ClientHttpResponse response) throws IOException {

            }
        });
    }
}