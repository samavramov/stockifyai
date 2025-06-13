<template>
  <div class="w-full h-full">
    <apexchart
      type="radialBar"
      :options="chartOptions"
      :series="[percent]"
      width="100%"
      height="100%"
    />
  </div>
</template>
<script>
import { defineComponent, computed } from 'vue';
import ApexCharts from 'vue3-apexcharts';

export default defineComponent({
  name: 'Gauge',
  components: { apexchart: ApexCharts },
  props: {
    displayValue: {
      type: Number,
      required: true
    },
    min: {
      type: Number,
      default: -1
    },
    max: {
      type: Number,
      default: 1
    }
  },
  setup(props) {
    const percent = computed(() => {
      const range = props.max - props.min;        
      const normalized = (props.displayValue - props.min) / range;
      const pct = normalized * 100;                     
      return Math.min(Math.max(pct, 0), 100);           
    });
    const fillColor = computed(() => {
      return props.displayValue < 0 ? '#FF0000' : '#20E647';
    });
    const chartOptions = computed(() => ({
      chart: {
        height: '100%',
        width: '100%',
        type: 'radialBar'
      },
      colors: [fillColor.value],    
      plotOptions: {
        radialBar: {
          startAngle: -90,
          endAngle: 90,
          hollow: {
            margin: 0,
            size: '70%',
            background: '#abada8'   
          },
          track: {
            background: '#f2f2f2',
            dropShadow: {
              enabled: true,
              top: 2,
              left: 0,
              blur: 4,
              opacity: 0.15
            }
          },
          dataLabels: {
            name: {
              offsetY: -10,
              color: '#fff',
              fontSize: '28px',
              fontFamily: 'Montserrat, sans-serif', 
              fontWeight: 'bold'
            },
            value: {
              color: '#fff',
              fontSize: '30px',
              show: true,
              fontFamily: 'Montserrat, sans-serif',
              formatter: () => props.displayValue.toFixed(2)
            }
          }
        }
      },
      fill: {
        type: 'solid',
        colors: [fillColor.value]
      },
      stroke: {
        lineCap: 'round'
      },
      labels: ['Sentiment Score']
    }));
    return {
      percent,
      chartOptions
    };
  }
});
</script>
<style scoped/>

