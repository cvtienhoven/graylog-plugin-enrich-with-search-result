package org.graylog.plugins;


import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.*;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EnrichWithSearchResultFunction extends AbstractFunction<String> {
    private static final Logger LOG = LoggerFactory.getLogger(EnrichWithSearchResultFunction.class);

    private static Searches searches;
    private static ClusterConfigService clusterConfigService;

    public static final String NAME = "enrich_with_search";
    public static final String STREAM_ID = "stream_id";
    public static final String QUERY = "query";
    public static final String SOURCE_FIELD = "source_field";
    public static final String DESTINATION_FIELD = "destination_field";
    public static final String MAX_MESSAGES = "max_messsages";
    public static final String MAX_MINUTES = "max_minutes";
    public static final String USE_SEQUENCE = "use_sequence";

    @Inject
    EnrichWithSearchResultFunction(Searches searches, ClusterConfigService clusterConfigService) {
        super();
        this.searches = searches;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {


            LOG.debug("Starting Evaluate");
            String streamId = streamParam.required(args, context);
            String query = queryParam.required(args, context);
            String sourceField = sourceFieldParam.required(args, context);
            String destinationField = destinationFieldParam.required(args, context);
            int maxMessages = Math.toIntExact(maxMessagesParam.required(args, context));
            int maxMinutes = Math.toIntExact(maxMinutesParam.required(args, context));
            boolean use_sequence = useSequenceParam.required(args, context);

            final TimeRange timeRange = buildRelativeTimeRange(60 * maxMinutes);
            String filter = "streams:" + streamId;

            LOG.info("Executing query [{}] with filter [{}]", query, filter);

            SearchResult searchResult = searches.search(
                    query,
                    filter,
                    timeRange,
                    maxMessages,
                    0,
                    new Sorting(Message.FIELD_TIMESTAMP, Sorting.Direction.DESC)
            );

            LOG.info("Query finished");

            List<String> values = new ArrayList<String>();

            int index = 0;
            for (ResultMessage resultMessage : searchResult.getResults()) {
                final Message msg = resultMessage.getMessage();
                if (msg.hasField(sourceField)) {
                    String value = String.valueOf(msg.getField(sourceField));
                    //prevent duplicates
                    if (!values.contains((value))) {
                        values.add(value);
                    }
                }
            }


            for (String value : values) {
                String fieldName = destinationField;
                if (use_sequence) {
                    fieldName += index;
                }
                LOG.debug("Adding field [{}] with value [{}]", fieldName, value);
                context.currentMessage().addField(fieldName, value);

                index++;

            }
            return "";//searchResult.getResults().size();

    }

    @Override
    public FunctionDescriptor descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .description("Finds messages")
                .params(streamParam, queryParam, sourceFieldParam, destinationFieldParam, maxMessagesParam, maxMinutesParam, useSequenceParam)
                .returnType(String.class)
                .build();
    }

    private final ParameterDescriptor<String, String> streamParam = ParameterDescriptor
            .string(STREAM_ID)
            .description("The stream ID to search in")
            .build();

    private final ParameterDescriptor<String, String> queryParam = ParameterDescriptor
            .string(QUERY)
            .description("The string to search for")
            .build();

    private final ParameterDescriptor<String, String> sourceFieldParam = ParameterDescriptor
            .string(SOURCE_FIELD)
            .description("The field of to use from the found messages")
            .build();

    private final ParameterDescriptor<String, String> destinationFieldParam = ParameterDescriptor
            .string(DESTINATION_FIELD)
            .description("The field name for the current message.")
            .build();

    private final ParameterDescriptor<Long, Long> maxMessagesParam = ParameterDescriptor
            .integer(MAX_MESSAGES)
            .description("The maximum number of messages to find")
            .build();

    private final ParameterDescriptor<Long, Long> maxMinutesParam = ParameterDescriptor
            .integer(MAX_MINUTES)
            .description("The maximum number of minutes to look back")
            .build();

    private final ParameterDescriptor<Boolean, Boolean> useSequenceParam = ParameterDescriptor
            .bool(USE_SEQUENCE)
            .description("Whether to use a sequence number in the destination field name for multiple results, e.g. field0, field1 etc.")
            .build();


    @VisibleForTesting
    TimeRange buildRelativeTimeRange(int range) {
        try {
            return restrictTimeRange(RelativeRange.create(range));
        } catch (InvalidRangeParametersException e) {
            LOG.warn("Invalid timerange parameters provided, not executing rule");
            return null;
        }
    }

    protected org.graylog2.plugin.indexer.searches.timeranges.TimeRange restrictTimeRange(
            final org.graylog2.plugin.indexer.searches.timeranges.TimeRange timeRange) {
        final DateTime originalFrom = timeRange.getFrom();
        final DateTime to = timeRange.getTo();
        final DateTime from;

        final SearchesClusterConfig config = clusterConfigService.get(SearchesClusterConfig.class);

        if (config == null || Period.ZERO.equals(config.queryTimeRangeLimit())) {
            from = originalFrom;
        } else {
            final DateTime limitedFrom = to.minus(config.queryTimeRangeLimit());
            from = limitedFrom.isAfter(originalFrom) ? limitedFrom : originalFrom;
        }

        return AbsoluteRange.create(from, to);
    }
}
