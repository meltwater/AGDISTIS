package org.aksw.agdistis.util;

import com.meltwater.fhai.kg.ned.agdistis.model.InputEntity;
import com.meltwater.fhai.kg.ned.agdistis.model.Occurrence;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Utils {

    private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static int[] convertIntegers(final List<Integer> integers) {
        final int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    public static Document textToDocument(final String documentId, final String plainText, final Map<Occurrence,
            InputEntity> entities) {
        final ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
        LOGGER.debug("Input text: " + plainText);
        LOGGER.debug("Input named entities: " + entities);
        for (final Occurrence occurrence : entities.keySet()) {
            final int start = occurrence.getStartOffset();
            final InputEntity entity = entities.get(occurrence);
            list.add(new NamedEntityInText(start, occurrence.getEndOffset() - start, entity.getName(), entity.getType
                    (), entity.getName()));
        }

        return documentFrom(Optional.of(documentId), plainText, list);
    }

    public static Document textToDocument(final String documentId, final String preAnnotatedText) {

        final ArrayList<NamedEntityInText> list = new ArrayList<NamedEntityInText>();
        LOGGER.debug("Input annotated text: " + preAnnotatedText);
        try {
            int startpos = 0, endpos = 0;
            final StringBuilder sb = new StringBuilder();
            startpos = preAnnotatedText.indexOf("<entity>", startpos);
            while (startpos >= 0) {
                sb.append(preAnnotatedText.substring(endpos, startpos));
                startpos += 8;
                endpos = preAnnotatedText.indexOf("</entity>", startpos);
                final int newStartPos = sb.length();

                final String entityLabel = preAnnotatedText.substring(startpos, endpos);

                list.add(new NamedEntityInText(newStartPos, entityLabel.length(), entityLabel, "", entityLabel));
                sb.append(entityLabel);
                endpos += 9;
                startpos = preAnnotatedText.indexOf("<entity>", startpos);
            }
        } catch (final IndexOutOfBoundsException iobe) {
            LOGGER.error("Error while processing text {}{}{}. StackTrace {}", IOUtils.LINE_SEPARATOR,
                    preAnnotatedText, IOUtils.LINE_SEPARATOR, ExceptionUtils.getStackTrace(iobe));
        }

        return documentFrom(Optional.of(documentId), preAnnotatedText.replaceAll("<entity>", "").replaceAll("</entity>", ""), list);

    }

    public static Document documentFrom(final Optional<String> documentId, final String text, final List<NamedEntityInText>
            entities) {
        final Document document = new Document();
        document.addText(text);
        document.addNamedEntitiesInText(new NamedEntitiesInText(entities));
        if (documentId.isPresent()) {
            document.setDocumentId(documentId.get());
        }
        return document;
    }
}
