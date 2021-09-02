package org.jetlinks.zlan.protocol.message;

import lombok.Data;

import java.util.List;

/**
 * @author Tensai
 */
@Data
public class ZlanMessagePack {

    private String id;

    private MessageType type;

    private String key;

    private Long time;

    private List<ChildMessagePack> data;

}
