/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.privacy.config.store.features.trackinglinkdetection

import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.app.runBlocking
import com.duckduckgo.privacy.config.store.*
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList

class RealTrackingLinkDetectionRepositoryTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    lateinit var testee: RealTrackingLinkDetectionRepository

    private val mockDatabase: PrivacyConfigDatabase = mock()
    private val mockTrackingLinkDetectionDao: TrackingLinkDetectionDao = mock()

    @Before
    fun before() {
        whenever(mockDatabase.trackingLinkDetectionDao()).thenReturn(mockTrackingLinkDetectionDao)
        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )
    }

    @Test
    fun whenRepositoryIsCreatedThenValuesLoadedIntoMemory() {
        givenTrackingLinkDetectionDaoContainsEntities()

        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )

        assertEquals(trackingLinkExceptionEntity.toTrackingLinkException(), testee.exceptions.first())
        assertEquals(ampLinkFormatEntity.format, testee.ampLinkFormats.first().toString())
        assertEquals(ampKeywordEntity.keyword, testee.ampKeywords.first())
        assertEquals(trackingParameterEntity.parameter, testee.trackingParameters.first())
    }

    @Test
    fun whenUpdateAllThenUpdateAllCalled() = coroutineRule.runBlocking {
        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )

        testee.updateAll(listOf(), listOf(), listOf(), listOf())

        verify(mockTrackingLinkDetectionDao).updateAll(anyList(), anyList(), anyList(), anyList())
    }

    @Test
    fun whenUpdateAllThenPreviousValuesAreCleared() = coroutineRule.runBlocking {
        givenTrackingLinkDetectionDaoContainsEntities()

        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )
        assertEquals(1, testee.exceptions.size)
        assertEquals(1, testee.ampLinkFormats.size)
        assertEquals(1, testee.ampKeywords.size)
        assertEquals(1, testee.trackingParameters.size)

        reset(mockTrackingLinkDetectionDao)

        testee.updateAll(listOf(), listOf(), listOf(), listOf())

        assertEquals(0, testee.exceptions.size)
        assertEquals(0, testee.ampLinkFormats.size)
        assertEquals(0, testee.ampKeywords.size)
        assertEquals(0, testee.trackingParameters.size)
    }

    @Test
    fun whenExtractCanonicalFromTrackingLinkAndUrlIsNotExtractableAmpLinkThenReturnNull() {
        givenTrackingLinkDetectionDaoContainsEntities()
        whenever(mockTrackingLinkDetectionDao.getAllAmpLinkFormats()).thenReturn(listOf(ampLinkFormatEntity))

        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )

        assertNull(testee.extractCanonicalFromTrackingLink("https://www.example.com"))
    }

    @Test
    fun whenExtractCanonicalFromTrackingLinkAndUrlIsExtractableAmpLinkThenReturnExtractedUrl() {
        givenTrackingLinkDetectionDaoContainsEntities()
        whenever(mockTrackingLinkDetectionDao.getAllAmpLinkFormats()).thenReturn(listOf(ampLinkFormatEntity))

        testee = RealTrackingLinkDetectionRepository(
            mockDatabase,
            TestCoroutineScope(),
            coroutineRule.testDispatcherProvider
        )

        val extractedLink = testee.extractCanonicalFromTrackingLink("https://www.google.com/amp/s/www.example.com")
        assertEquals("https://www.example.com", extractedLink)
    }

    private fun givenTrackingLinkDetectionDaoContainsEntities() {
        whenever(mockTrackingLinkDetectionDao.getAllExceptions()).thenReturn(listOf(trackingLinkExceptionEntity))
        whenever(mockTrackingLinkDetectionDao.getAllAmpLinkFormats()).thenReturn(listOf(ampLinkFormatEntity))
        whenever(mockTrackingLinkDetectionDao.getAllAmpKeywords()).thenReturn(listOf(ampKeywordEntity))
        whenever(mockTrackingLinkDetectionDao.getAllTrackingParameters()).thenReturn(listOf(trackingParameterEntity))
    }

    companion object {
        val trackingLinkExceptionEntity = TrackingLinkExceptionEntity(
            domain = "https://www.example.com",
            reason = "reason"
        )

        val ampLinkFormatEntity = AmpLinkFormatEntity(
            format = "https?:\\/\\/(?:w{3}\\.)?google\\.\\w{2,}\\/amp\\/s\\/(\\S+)"
        )

        val ampKeywordEntity = AmpKeywordEntity(
            keyword = "keyword"
        )

        val trackingParameterEntity = TrackingParameterEntity(
            parameter = "parameter"
        )
    }
}