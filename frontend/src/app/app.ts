import { Component, ElementRef, ViewChild } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { forkJoin, of } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  @ViewChild('projectionCanvas') projectionCanvas?: ElementRef<HTMLCanvasElement>;

  selectedFund = 'VSMPX';
  amount = 0;
  years = 0;
  result: number | null = null;
  error: string | null = null;
  loading = false;
  private chart: Chart | null = null;

  constructor(private http: HttpClient) {}

  calculate() {
    this.error = null;
    this.loading = true;
    this.result = null;
    this.chart?.destroy();
    this.chart = null;

    if (this.amount <= 0 || this.years <= 0) {
      this.error = 'Please enter amount and years greater than 0.';
      this.loading = false;
      return;
    }

    const timePoints = this.buildTimePoints(this.years);
    const requests = timePoints.map((timePoint) => {
      if (timePoint === 0) {
        return of(this.amount);
      }

      const params = new HttpParams()
        .set('principal', this.amount)
        .set('years', timePoint.toString());

      return this.http.get<number>(`http://localhost:8080/funds/future-value/${this.selectedFund}`, { params });
    });

    forkJoin(requests).subscribe({
      next: (values) => {
        this.result = values[values.length - 1] ?? null;
        setTimeout(() => this.renderProjectionChart(timePoints, values), 0);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Could not reach the backend. Is the Spring server running on port 8080?';
        this.loading = false;
        console.error(err);
      }
    });
  }

  private buildTimePoints(years: number): number[] {
    const points: number[] = [0];
    const wholeYears = Math.floor(years);

    for (let year = 1; year <= wholeYears; year++) {
      points.push(year);
    }

    if (!Number.isInteger(years)) {
      points.push(years);
    }

    return points;
  }

  private renderProjectionChart(timePoints: number[], values: number[]) {
    if (!this.projectionCanvas?.nativeElement || timePoints.length === 0 || values.length === 0) {
      return;
    }

    const labels = timePoints.map((value) => Number.isInteger(value) ? `Year ${value}` : `Year ${value.toFixed(1)}`);
    const data = values.map((value) => Number(value.toFixed(2)));

    const ctx = this.projectionCanvas.nativeElement.getContext('2d');
    if (!ctx) {
      return;
    }

    this.chart?.destroy();
    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: `${this.selectedFund} projected value`,
            data,
            borderColor: '#007f08',
            backgroundColor: 'rgba(103, 172, 107, 0.15)',
            fill: true,
            tension: 0.25,
            pointRadius: 3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false
      }
    });
  }
}