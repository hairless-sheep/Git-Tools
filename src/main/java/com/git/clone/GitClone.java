package com.git.clone;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GitClone {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        // 获取键盘输入 - GitLab仓库URL地址
        String gitlabUrl = getGitlabUrl(scanner);
        // 获取键盘输入 - GitLab仓库Access Token
        String accessToken = getAccessToken(scanner);
        // 调用GitLab API获取当前Access Token有权限访问的项目信息数据
        JSONArray gitLabProjects = getGitLabProjects(gitlabUrl, accessToken, null);
        // 打印输出获取到的项目Http Clone地址
        printlnCloneHttpUrls(gitLabProjects);
        // 获取键盘输入 - 是否需要进行项目地址分组Clone
        Boolean groupBy = getGroupBy(scanner);
        // 获取键盘输入 - 是否需要指定分支Clone
        Boolean needBranch = getNeedBranch(scanner);
        String targetBranch = null;
        if (needBranch) {
            // 获取键盘输入 - 目标Clone分支
            targetBranch = getTargetBranch(scanner);
        }
        // 获取Clone路径
        String diskPath = getDiskPath(scanner);
        // clone project
        System.out.println("=======================CLONE PROJECT START=======================");
        for (Object projectObj : gitLabProjects) {
            JSONObject projectJson = JSON.parseObject(JSON.toJSONString(projectObj));
            String httpUrlToRepo = (String) projectJson.get("http_url_to_repo");
            String clonePath = diskPath.endsWith(File.separator) ? diskPath.substring(0, diskPath.length() - 1) : diskPath;
            if (groupBy) {
                clonePath = buildDirPath(projectJson, clonePath);
            }
            String command = "git clone " + httpUrlToRepo + (StringUtils.isNotBlank(targetBranch) ? " -b " + targetBranch : "")
                    + " " + clonePath;
            System.out.println(command);
            // clone命令 ProcessBuilder传入的命令数组相当于空格拆分命令
            ProcessBuilder processBuilder = new ProcessBuilder(Lists.newArrayList(command.split(" ")));
            // 合并 错误流和标准流
            processBuilder.redirectErrorStream(true);
            try {
                Process process = processBuilder.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("=======================CLONE PROJECT FINISH=======================");
    }

    private static String buildDirPath(JSONObject projectJson, String clonePath) {
        String pathWithNamespace = (String) projectJson.get("path_with_namespace");
        String[] split = pathWithNamespace.split("/");
        for (int i = 0; i < split.length; i++) {
            clonePath = clonePath + File.separator + split[i];
        }
        return clonePath;
    }

    private static String getDiskPath(Scanner scanner) {
        File directory = new File("");
        String absolutePath = directory.getAbsolutePath();
        System.out.println("当前项目Clone到的磁盘路径为【" + absolutePath + "】，您是否需求更换Clone到的磁盘路径【Y=是|N=否】:");
        String choose = null;
        if (scanner.hasNextLine()) {
            choose = scanner.nextLine();
        }
        while (!"y".equalsIgnoreCase(choose) && !"n".equalsIgnoreCase(choose)) {
            System.out.println("请勿输入除选项外的字符！请您再次输入【Y=是|N=否】:");
            if (scanner.hasNextLine()) {
                choose = scanner.nextLine();
            }
        }
        Boolean changePath = ("y".equalsIgnoreCase(choose) ? true : false);
        if (changePath) {
            System.out.println("请输入您要Clone到的磁盘路径:");
            String targetPath = null;
            if (scanner.hasNextLine()) {
                targetPath = scanner.nextLine();
            }
            while (StringUtils.isBlank(targetPath)) {
                System.out.println("磁盘路径不可为空或为无效地址(不存在或非文件夹)！请输入您要Clone到的磁盘路径:");
                if (scanner.hasNextLine()) {
                    targetPath = scanner.nextLine();
                }
                // 校验磁盘路径是否有效
                File file = new File(targetPath);
                if (!file.exists()) {
                    targetPath = null;
                }
                if (!file.isDirectory()) {
                    targetPath = null;
                }
            }
            absolutePath = targetPath;
        }
        return absolutePath;
    }

    private static String getTargetBranch(Scanner scanner) {
        System.out.println("请输入您要Clone的目标分支:");
        String targetBranch = null;
        if (scanner.hasNextLine()) {
            targetBranch = scanner.nextLine();
        }
        while (StringUtils.isBlank(targetBranch)) {
            System.out.println("目标分支不可为空！请输入您要Clone的目标分支:");
            if (scanner.hasNextLine()) {
                targetBranch = scanner.nextLine();
            }
        }
        return targetBranch;
    }

    private static Boolean getNeedBranch(Scanner scanner) {
        System.out.println("请输入您是否需要Clone指定分支【Y=是|N=否】:");
        String choose = null;
        if (scanner.hasNextLine()) {
            choose = scanner.nextLine();
        }
        while (!"y".equalsIgnoreCase(choose) && !"n".equalsIgnoreCase(choose)) {
            System.out.println("请勿输入除选项外的字符！请您再次输入【Y=是|N=否】:");
            if (scanner.hasNextLine()) {
                choose = scanner.nextLine();
            }
        }
        Boolean needBranch = ("y".equalsIgnoreCase(choose) ? true : false);
        return needBranch;
    }

    private static Boolean getGroupBy(Scanner scanner) {
        System.out.println("请输入您是否需要按照项目分组进行Clone操作【Y=是|N=否】:");
        String choose = null;
        if (scanner.hasNextLine()) {
            choose = scanner.nextLine();
        }
        while (!"y".equalsIgnoreCase(choose) && !"n".equalsIgnoreCase(choose)) {
            System.out.println("请勿输入除选项外的字符！请您再次输入【Y=是|N=否】:");
            if (scanner.hasNextLine()) {
                choose = scanner.nextLine();
            }
        }
        Boolean groupBy = ("y".equalsIgnoreCase(choose) ? true : false);
        return groupBy;
    }

    private static void printlnCloneHttpUrls(JSONArray gitLabProject) {
        System.out.println("获取GitLab Project成功！共抓取到可Clone项目量【" + gitLabProject.size() + "】个");
        System.out.println("=======================CLONE HTTP URL START=======================");
        for (Object obj : gitLabProject) {
            JSONObject jsonObj = JSON.parseObject(JSON.toJSONString(obj));
            String httpUrlToRepo = (String) jsonObj.get("http_url_to_repo");
            System.out.println(httpUrlToRepo);
        }
        System.out.println("=======================CLONE HTTP URL END=======================");
    }

    private static String getAccessToken(Scanner scanner) {
        System.out.println("请输入您在GitLab上生成的Access Tokens:");
        String accessToken = null;
        if (scanner.hasNextLine()) {
            accessToken = scanner.nextLine();
        }
        while (StringUtils.isBlank(accessToken)) {
            System.out.println("Access Tokens不可为空！请输入您在GitLab上生成的Access Tokens:");
            if (scanner.hasNextLine()) {
                accessToken = scanner.nextLine();
            }
        }
        return accessToken;
    }

    private static String getGitlabUrl(Scanner scanner) {
        System.out.println("请输入您要Clone的GitLab地址【例如 https://github.com】:");
        String gitlabUrl = null;
        if (scanner.hasNextLine()) {
            gitlabUrl = scanner.nextLine();
        }
        while (StringUtils.isBlank(gitlabUrl)) {
            System.out.println("GitLab地址不可为空！请输入您要Clone的GitLab地址:");
            if (scanner.hasNextLine()) {
                gitlabUrl = scanner.nextLine();
            }
        }
        return gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1) : gitlabUrl;
    }

    /**
     * 抓取GitLab有权限访问的项目
     *
     * @param gitlabUrl
     * @param accessToken
     * @return
     * @throws IOException
     */
    private static JSONArray getGitLabProjects(String gitlabUrl, String accessToken, String pageNo) throws IOException {
        if (StringUtils.isBlank(gitlabUrl) || StringUtils.isBlank(accessToken)) {
            throw new RuntimeException("GitLab URL地址和Access Tokens均不可为空！");
        }
        BufferedReader reader = null;
        InputStreamReader streamReader = null;
        try {
            URL url = new URL(gitlabUrl + "/api/v4/projects?membership=true&per_page=100&page=" + (StringUtils.isBlank(pageNo) ? "1" : pageNo));
            // 得到链接对象
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // 设置请求类型
            conn.setRequestMethod("GET");
            // 设置请求头
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            // 允许写出
            conn.setDoOutput(true);
            // 允许读入
            conn.setDoInput(true);
            // 不使用缓存
            conn.setUseCaches(false);
            // 判断响应状态码
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("访问GitLab API的响应状态码为" + conn.getResponseCode() + "，请确认该GitLab地址是否可以访问！");
            }
            // 获取响应结果
            streamReader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            reader = new BufferedReader(streamReader);
            StringBuffer buffer = new StringBuffer();
            String line;
            while (null != (line = reader.readLine())) {
                buffer.append(line);
            }
            JSONArray resultArray = JSON.parseArray(buffer.toString());
            // 递归获取数据 - GitLab一次默认设置仅100条数据
            Map<String, List<String>> headerFields = conn.getHeaderFields();
            String xNextPage = headerFields.get("X-Next-Page").get(0);
            if (StringUtils.isNotBlank(xNextPage)) {
                JSONArray innerResultArray = getGitLabProjects(gitlabUrl, accessToken, xNextPage);
                if (null != innerResultArray && !innerResultArray.isEmpty()) {
                    resultArray.addAll(innerResultArray);
                }
            }
            return resultArray;
        } catch (Exception e) {
            throw new RuntimeException(String.format("访问GitLab时发生异常：%s", e.getMessage()), e);
        } finally {
            if (null != reader) {
                reader.close();
            }
            if (null != streamReader) {
                streamReader.close();
            }
        }
    }
}