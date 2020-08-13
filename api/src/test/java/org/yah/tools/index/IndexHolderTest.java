package org.yah.tools.index;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class IndexHolderTest {

    @Test
    public void test_single_reader() {
        Index<String> index = mock(Index.class);
        IndexReader<String> reader = mock(IndexReader.class);
        when(index.openReader(null)).thenReturn(reader);
        final IndexHolder<String> holder = new IndexHolder<>(index);
        assertThat(holder.reader(), is(reader));
        assertThat(holder.reader(), is(reader));
        verify(index, times(1)).openReader(null);

        holder.close();
        assertThat(holder.reader(), is(reader));
        verify(index, times(2)).openReader(null);
    }

    @Test
    public void test_writer_then_reader() {
        Index<String> index = mock(Index.class);
        IndexWriter<String> writer = mock(IndexWriter.class);
        IndexReader<String> reader = mock(IndexReader.class);
        final IndexHolder<String> holder = new IndexHolder<>(index);

        when(index.openWriter()).thenReturn(writer);
        when(index.openReader(null)).thenReturn(reader);
        when(index.openReader(writer)).thenReturn(reader);

        assertThat(holder.writer(), is(writer));
        assertThat(holder.writer(), is(writer));

        assertThat(holder.reader(), is(reader));
        assertThat(holder.reader(), is(reader));

        verify(index, times(1)).openWriter();
        verify(index, times(1)).openReader(writer);

        holder.close();
        assertThat(holder.reader(), is(reader));
        verify(index, times(1)).openReader(null);
        assertThat(holder.writer(), is(writer));
        verify(index, times(2)).openWriter();
        assertThat(holder.reader(), is(reader));
        verify(index, times(2)).openReader(writer);
    }

    @Test
    public void test_close_prevented() {
        Index<String> index = mock(Index.class);
        IndexWriter<String> writer = mock(IndexWriter.class);
        IndexReader<String> reader = mock(IndexReader.class);
        when(index.openWriter()).thenReturn(writer);
        when(index.openReader(any())).thenReturn(reader);

        final IndexHolder<String> holder = new IndexHolder<>(index);
        holder.writer();
        holder.reader();

        holder.reader().close();
        holder.writer().close();
        verify(reader, never()).close();
        verify(writer, never()).close();
    }


}