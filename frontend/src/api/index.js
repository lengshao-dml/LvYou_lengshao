import axios from 'axios';

const apiClient = axios.create({
  // 由于我们配置了Vite代理，这里可以直接使用相对路径
  // 所有/api开头的请求都会被代理到 http://localhost:8080/api
  baseURL: '/api', 
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * 获取所有标签
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getTags = () => {
  return apiClient.get('/tags');
};

/**
 * 获取旅游推荐
 * @param {object} recommendationRequest - 推荐请求的数据传输对象
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getRecommendations = (recommendationRequest) => {
  return apiClient.post('/recommend', recommendationRequest);
};

export const getCities = () => {
    return apiClient.get('/cities');
};

export const getCityByName = (name) => {
    return apiClient.get(`/city/${name}`);
};

export const getPopularCities = () => {
    return apiClient.get('/cities/popular');
};

export const logClickEvent = (cityId, token) => {
    return apiClient.post('/events/click', { cityId }, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
};
