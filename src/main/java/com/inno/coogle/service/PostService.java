package com.inno.coogle.service;

import com.inno.coogle.domain.Image;
import com.inno.coogle.domain.Member;
import com.inno.coogle.domain.Post;
import com.inno.coogle.dto.post.PostRequestDto;
import com.inno.coogle.dto.post.PostResponseDto;
import com.inno.coogle.global.error.exception.ErrorCode;
import com.inno.coogle.global.error.exception.InvalidValueException;
import com.inno.coogle.repository.ImageRepository;
import com.inno.coogle.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final AmazonS3Service amazonS3Service;

    // 게시글 작성
    @Transactional
    public void createPost(PostRequestDto postRequestDto, Member member, List<MultipartFile> imageFileList) {
        List<String> imageUrlList = amazonS3Service.uploadFile(imageFileList, "posts/");
        Post post = new Post(member, postRequestDto);
        postRepository.save(post);
        List<Image> collect = imageUrlList.stream().map(image -> new Image(image, post)).collect(Collectors.toList());
        imageRepository.saveAll(collect);
        post.setThumbnailUrl(collect.get(0).getImageUrl());
    }

    // 전체 게시글 리스트 조회
    @Transactional
    public List<PostResponseDto> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByModifiedAtDesc();
        List<PostResponseDto> postResponseDtoList = new ArrayList<>();
        for (Post post : posts) {
            postResponseDtoList.add(PostResponseDto.builder()
                    .post(post)
                    .build());
        }
        return postResponseDtoList;
    }




    // 게시글 수정
    @Transactional
    public void updatePost(Long postId, PostRequestDto postRequestDto, Member member, List<MultipartFile> imageFileList) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new InvalidValueException(ErrorCode.NOTFOUND_POST));
        if (!member.getMemberId().equals(post.getMember().getMemberId())) {
            throw new InvalidValueException(ErrorCode.NOT_AUTHORIZED);
        }

        List<String> imageUrlList = amazonS3Service.uploadFile(imageFileList, "posts/");
        post.update(postRequestDto);
        List<Image> collect = imageUrlList.stream().map(image -> new Image(image, post)).collect(Collectors.toList());
        imageRepository.saveAll(collect);
    }

    // 게시글 삭제
    public void deletePost(Long postId, Member member) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new InvalidValueException(ErrorCode.NOTFOUND_POST));
        if (!member.getMemberId().equals(post.getMember().getMemberId())) {
            throw new InvalidValueException(ErrorCode.NOT_AUTHORIZED);
        }
        postRepository.deleteById(postId);
    }



}