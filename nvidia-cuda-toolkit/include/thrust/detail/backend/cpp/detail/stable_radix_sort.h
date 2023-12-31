/*
 *  Copyright 2008-2011 NVIDIA Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#pragma once

namespace thrust
{
namespace detail
{
namespace backend
{
namespace cpp
{
namespace detail
{

template<typename RandomAccessIterator>
void stable_radix_sort(RandomAccessIterator begin,
                       RandomAccessIterator end);

template<typename RandomAccessIterator1,
         typename RandomAccessIterator2>
void stable_radix_sort_by_key(RandomAccessIterator1 keys_begin,
                              RandomAccessIterator1 keys_end,
                              RandomAccessIterator2 values_begin);

} // end namespace detail
} // end namespace cpp
} // end namespace backend
} // end namespace detail
} // end namespace thrust

#include <thrust/detail/backend/cpp/detail/stable_radix_sort.inl>

