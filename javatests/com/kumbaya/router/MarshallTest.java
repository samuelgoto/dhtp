package com.kumbaya.router;

import com.google.common.collect.ImmutableList;
import com.kumbaya.router.Marshall.TLV;
import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Assert;

import static com.kumbaya.router.Marshall.marshall;
import static com.kumbaya.router.Marshall.unmarshall;

public class MarshallTest extends TestCase {
  
  public void testMarshallingAndUnmarshalling() throws IOException {
    TLV data = TLV.of(0xAB, "hello world".getBytes());    
    List<TLV> result = unmarshall(marshall(ImmutableList.of(data)));
    assertEquals(1, result.size());
    assertEquals(data.type, result.get(0).type);
    Assert.assertArrayEquals(data.content, result.get(0).content);   
  }

  public void testMarshallingAndUnmarshalling_multipleFields() throws IOException {
    List<TLV> result = unmarshall(marshall(ImmutableList.of(
        TLV.of(0xAB, "hello".getBytes()),
        TLV.of(0xCD, "world".getBytes()))));
    assertEquals(2, result.size());
    assertEquals(0xAB, result.get(0).type);
    Assert.assertArrayEquals("hello".getBytes(), result.get(0).content); 
    assertEquals(0xCD, result.get(1).type);
    Assert.assertArrayEquals("world".getBytes(), result.get(1).content); 
  }

  public void testMarshallingAndUnmarshalling_longText() throws IOException {
    String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse rhoncus, "
        + "libero at congue varius, odio arcu rutrum velit, et laoreet arcu ligula sit amet dui. "
        + "Suspendisse ornare nunc quis purus molestie, a finibus purus tempor. Donec id diam sed"
        + " mi fermentum tempus eget et nisl. Vivamus vestibulum tristique nisi vitae dignissim. "
        + "Nulla vehicula nisi nec est convallis aliquam. Sed commodo, nunc placerat posuere posu"
        + "ere, enim nunc venenatis orci, id pellentesque leo nunc eu felis. Donec eu mi dapibus,"
        + " rhoncus nisl varius, auctor felis. Aenean convallis, ligula eget bibendum tristique, "
        + "erat justo feugiat nisi, eu porta nibh quam non neque. Nulla tempus placerat neque, et"
        + " vestibulum eros molestie quis. Integer porttitor vestibulum facilisis. Vestibulum int"
        + "erdum interdum tincidunt. Ut mauris ligula, pretium nec dui ac, convallis porta ipsum."
        + " Proin blandit enim eget risus bibendum, vitae imperdiet magna iaculis. Nullam malesua"
        + "da enim eget risus elementum, a malesuada magna commodo. Sed tempus porttitor leo. Pro"
        + "in sollicitudin dictum nibh, sit amet dignissim diam commodo id.";
    
    TLV data = TLV.of(0xAB, text.getBytes());    
    List<TLV> result = unmarshall(marshall(ImmutableList.of(data)));
    assertEquals(1, result.size());
    assertEquals(data.type, result.get(0).type);
    Assert.assertArrayEquals(data.content, result.get(0).content);
  }
}
