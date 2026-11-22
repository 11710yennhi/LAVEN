package iuh.fit.laven.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Danh sách món của giỏ localStorage (dùng cho preview và merge). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemsRequest {
    private List<CartItemRequest> items;
}
