package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardBeanWithRef extends CardBean {
    private Plan plan;
    private CardBean parent;
    private StoryMap storyMap;
    private StoryMapNode storyMapL1Node;
    private StoryMapNode storyMapL2Node;
}
