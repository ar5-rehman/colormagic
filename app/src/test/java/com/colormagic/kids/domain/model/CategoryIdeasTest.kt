package com.colormagic.kids.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryIdeasTest {

    @Test
    fun builtInIdeasMapToTheirOwnCategory() {
        CategoryIdeas.allIdeaItems.forEach { idea ->
            assertEquals(idea.text, idea.category, CategoryIdeas.categorize(idea.text))
        }
    }

    @Test
    fun freeTextPromptsUseKeywords() {
        assertEquals(CategoryIdeas.MAGIC, CategoryIdeas.categorize("a friendly dragon chef"))
        assertEquals(CategoryIdeas.DINOSAURS, CategoryIdeas.categorize("Baby T-Rex playing football"))
        assertEquals(CategoryIdeas.ROBOTS, CategoryIdeas.categorize("a robot dog"))
        assertEquals(CategoryIdeas.SPACE, CategoryIdeas.categorize("cat astronaut on the moon"))
        assertEquals(CategoryIdeas.VEHICLES, CategoryIdeas.categorize("Fire trucks racing"))
        assertEquals(CategoryIdeas.ANIMALS, CategoryIdeas.categorize("Two puppies"))
        assertEquals(CategoryIdeas.NATURE, CategoryIdeas.categorize("A Smiling Sunflower In"))
    }

    @Test
    fun unknownOrEmptyPromptsHaveNoCategory() {
        assertNull(CategoryIdeas.categorize("my birthday party"))
        assertNull(CategoryIdeas.categorize(""))
        assertNull(CategoryIdeas.categorize(null))
    }
}
