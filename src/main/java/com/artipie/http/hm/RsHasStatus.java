/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.http.hm;

import com.artipie.http.Connection;
import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.reactivestreams.Publisher;

/**
 * Matcher to verify response status.
 * @since 0.1
 */
public final class RsHasStatus extends TypeSafeMatcher<Response> {

    /**
     * Status code matcher.
     */
    private final Matcher<Integer> status;

    /**
     * Ctor.
     * @param status Code to match
     */
    public RsHasStatus(final int status) {
        this(new IsEqual<>(status));
    }

    /**
     * Ctor.
     * @param status Code matcher
     */
    public RsHasStatus(final Matcher<Integer> status) {
        this.status = status;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.status);
    }

    @Override
    public boolean matchesSafely(final Response item) {
        final AtomicInteger out = new AtomicInteger();
        try {
            item.send(new FakeConnection(out)).toCompletableFuture().get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new IllegalArgumentException("Bad response", ex);
        }
        return this.status.matches(out.get());
    }

    /**
     * Fake connection.
     * @since 0.1
     */
    private static final class FakeConnection implements Connection {

        /**
         * Status code container.
         */
        private final AtomicInteger container;

        /**
         * Ctor.
         * @param container Status code container
         */
        FakeConnection(final AtomicInteger container) {
            this.container = container;
        }

        @Override
        public CompletableFuture<Void> accept(
            final int code,
            final Iterable<Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            return CompletableFuture.supplyAsync(
                () -> {
                    this.container.set(code);
                    return null;
                }
            );
        }
    }
}
