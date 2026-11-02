package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.community.Comment;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PostTest {

    private final Member user = ApplicationFixtures.applicant(1L);

    @Test
    @DisplayName("공지는 관리자만 쓸 수 있고, 그 밖의 분류는 누구나")
    void noticeAdminOnly() {
        assertThatThrownBy(() -> Post.write(user, PostCategory.NOTICE, "공지", "내용"))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTICE_ADMIN_ONLY);

        Member admin = ApplicationFixtures.applicant(3L);
        ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
        assertThat(Post.write(admin, PostCategory.NOTICE, " 서비스 점검 안내 ", "내용").getTitle()).isEqualTo("서비스 점검 안내");
    }

    @Test
    @DisplayName("수정은 작성자만, 공지로 바꿀 수 없다. 삭제는 작성자 또는 관리자")
    void editAndDelete() {
        Post post = Post.write(user, PostCategory.RECRUIT, "팀원 모집", "내용");

        assertThatThrownBy(() -> post.edit(2L, PostCategory.RECRUIT, "제목", "내용"))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        assertThatThrownBy(() -> post.edit(1L, PostCategory.NOTICE, "제목", "내용"))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTICE_ADMIN_ONLY);
        post.edit(1L, PostCategory.QUESTION, "질문으로 바꿈", "내용");
        assertThat(post.getCategory()).isEqualTo(PostCategory.QUESTION);

        assertThatThrownBy(() -> post.validateDeletableBy(2L, Role.USER))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_POST_AUTHOR);
        assertThatCode(() -> post.validateDeletableBy(3L, Role.ADMIN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 삭제는 작성자 또는 관리자, 숨기면 보이지 않는다")
    void comment() {
        Comment comment = new Comment(Post.write(user, PostCategory.QUESTION, "질문", "내용"), user, " 답변 ");

        assertThat(comment.getContent()).isEqualTo("답변");
        assertThatThrownBy(() -> comment.validateDeletableBy(2L, Role.PROVIDER))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_COMMENT_AUTHOR);
        comment.hide();
        assertThat(comment.isVisible()).isFalse();
    }
}
