#pragma once


typedef struct _moving_average_data
{
  float    average;
  float    *data;
  uint16_t idx;
  uint16_t count;
  uint16_t size;
  bool     alwaysAccurate;
} moving_average_data;

void ma_init(moving_average_data *ma, float *data, uint16_t size, bool alwaysAccurate)
{
  ma->size = size;
  ma->data = data;
  ma->average = 0.0f;
  ma->idx = 0;
  ma->count = 0;
  ma->alwaysAccurate = alwaysAccurate;

  uint16_t u; for(u=0; u < ma->size ; u++)
    ma->data[u] = 0.0f;
}

void ma_add(moving_average_data *ma, float fValue)
{
  float fRemoved = ma->data[ma->idx];
  ma->data[ma->idx] = fValue;
  ma->count ++;
  if(ma->alwaysAccurate && ma->count < ma->size )
  {
    ma->average = 0;
    uint32_t u; for (u=0; u < ma->count; u++)
      ma->average += ma->data[u]/ma->size ;
  }
  else
  {
    ma->average = ma->average - (fRemoved / ma->size ) + (fValue / ma->size );
  }
  ma->idx = (ma->idx+1) % ma->size ;
}

void ma_add_all(moving_average_data *ma, float fValue)
{
  ma->average = fValue;
  ma->count = ma->size ;
  ma->idx = 0;
  uint32_t u; for (u=0; u < ma->count; u++)
    ma->data[u] = fValue;
}

float ma_average(moving_average_data *ma)
{
  return ma->average;
}

